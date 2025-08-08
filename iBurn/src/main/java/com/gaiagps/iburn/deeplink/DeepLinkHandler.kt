package com.gaiagps.iburn.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gaiagps.iburn.IntentUtil
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.MapPin
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.getSharedDb
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class DeepLinkHandler(
    private val context: Context,
    private val dataProvider: DataProvider
) {
    
    companion object {
        private const val TAG = "DeepLinkHandler"
        
        // URL path types
        private const val PATH_ART = "art"
        private const val PATH_CAMP = "camp"
        private const val PATH_EVENT = "event"
        private const val PATH_PIN = "pin"
        
        // Intent extras for map pins
        const val ACTION_SHOW_MAP_PIN = "com.gaiagps.iburn.SHOW_MAP_PIN"
        const val EXTRA_PIN_ID = "pin_id"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }
    
    private val disposables = CompositeDisposable()
    
    fun canHandle(uri: Uri): Boolean {
        return when (uri.scheme) {
            "iburn" -> true
            "https", "http" -> {
                val host = uri.host
                host == "iburnapp.com" || host == "www.iburnapp.com"
            }
            else -> false
        }
    }
    
    fun handle(uri: Uri, callback: (Intent?) -> Unit) {
        if (!canHandle(uri)) {
            callback(null)
            return
        }
        
        val pathSegments = uri.pathSegments
        val queryParams = extractQueryParams(uri)
        
        when {
            // Handle https://iburnapp.com/art/?uid=xxx or /camp/?uid=xxx or /event/?uid=xxx
            pathSegments.isNotEmpty() && pathSegments[0] in listOf(PATH_ART, PATH_CAMP, PATH_EVENT) -> {
                val type = pathSegments[0]
                val uid = queryParams["uid"]
                
                if (uid != null) {
                    handleDataObject(type, uid, queryParams, callback)
                } else {
                    callback(null)
                }
            }
            // Handle https://iburnapp.com/pin
            pathSegments.isNotEmpty() && pathSegments[0] == PATH_PIN -> {
                handleMapPin(queryParams, callback)
            }
            // Handle iburn://art?uid=xxx style URLs (scheme-based)
            uri.scheme == "iburn" -> {
                val host = uri.host
                val uid = queryParams["uid"]
                
                if (host in listOf(PATH_ART, PATH_CAMP, PATH_EVENT) && uid != null) {
                    handleDataObject(host, uid, queryParams, callback)
                } else if (host == PATH_PIN) {
                    handleMapPin(queryParams, callback)
                } else {
                    callback(null)
                }
            }
            else -> {
                callback(null)
            }
        }
    }
    
    private fun extractQueryParams(uri: Uri): Map<String, String> {
        return uri.queryParameterNames.associateWith { name ->
            uri.getQueryParameter(name) ?: ""
        }
    }
    
    private fun handleDataObject(
        type: String,
        playaId: String,
        metadata: Map<String, String>,
        callback: (Intent?) -> Unit
    ) {
        val disposable = when (type) {
            PATH_ART -> dataProvider.observeArt(playaId)
            PATH_CAMP -> dataProvider.observeCamp(playaId)
            PATH_EVENT -> dataProvider.getEvent(playaId)
                .toFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
            else -> null
        }
        
        if (disposable != null) {
            disposables.add(
                disposable.subscribe(
                    { playaItem ->
                        if (playaItem != null) {
                            val intent = IntentUtil.viewItemDetail(context, playaItem as PlayaItem)
                            callback(intent)
                        } else {
                            Timber.w("Object not found: $type/$playaId")
                            callback(null)
                        }
                    },
                    { error ->
                        Timber.e(error, "Error loading deep link object: $type/$playaId")
                        callback(null)
                    }
                )
            )
        } else {
            callback(null)
        }
    }
    
    private fun handleMapPin(metadata: Map<String, String>, callback: (Intent?) -> Unit) {
        val lat = metadata["lat"]?.toDoubleOrNull()
        val lng = metadata["lng"]?.toDoubleOrNull()
        val title = metadata["title"] ?: "Custom Pin"
        
        if (lat == null || lng == null) {
            Timber.e("Invalid coordinates for pin: lat=$lat, lng=$lng")
            callback(null)
            return
        }
        
        // Validate coordinates are within Black Rock City bounds
        if (!isValidBRCCoordinate(lat, lng)) {
            Timber.e("Coordinates outside BRC bounds: $lat, $lng")
            callback(null)
            return
        }
        
        // Create and save the pin
        val pin = MapPin(
            uid = UUID.randomUUID().toString(),
            title = title,
            description = metadata["desc"],
            latitude = lat.toFloat(),
            longitude = lng.toFloat(),
            address = metadata["addr"],
            color = metadata["color"] ?: "red",
            createdAt = System.currentTimeMillis()
        )
        
        // Save pin to database
        val db = getSharedDb(context)
        val disposable = db.mapPinDao().insert(pin)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    // Create intent to show map centered on pin
                    val intent = Intent(ACTION_SHOW_MAP_PIN).apply {
                        putExtra(EXTRA_PIN_ID, pin.uid)
                        putExtra(EXTRA_LATITUDE, lat)
                        putExtra(EXTRA_LONGITUDE, lng)
                    }
                    callback(intent)
                },
                { error ->
                    Timber.e(error, "Error saving map pin")
                    callback(null)
                }
            )
        
        disposables.add(disposable)
    }
    
    private fun isValidBRCCoordinate(lat: Double, lng: Double): Boolean {
        // Black Rock City approximate bounds
        return lat in 40.75..40.82 && lng in -119.25..-119.17
    }
    
    fun dispose() {
        disposables.clear()
    }
}