package com.gaiagps.iburn.deeplink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gaiagps.iburn.IntentUtil
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.MapPin
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.getSharedDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
        const val EXTRA_PIN_TITLE = "pin_title"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
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
    
    fun handle(host: Activity, uri: Uri, callback: (Intent?) -> Unit) {
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
                    handleDataObject(host, type, uid, queryParams, callback)
                    return
                } else {
                    callback(null)
                    return
                }
            }
            // Handle https://iburnapp.com/pin
            pathSegments.isNotEmpty() && pathSegments[0] == PATH_PIN -> {
                handleMapPin(queryParams, callback)
            }
            // Handle iburn://art?uid=xxx style URLs (scheme-based)
            uri.scheme == "iburn" -> {
                val type = uri.host
                val uid = queryParams["uid"]

                if (type == null) {
                    Timber.e("Invalid iburn URL missing type : $uri")
                    callback(null)
                    return
                }
                
                if (type in listOf(PATH_ART, PATH_CAMP, PATH_EVENT) && uid != null) {
                    handleDataObject(host, type, uid, queryParams, callback)
                } else if (type == PATH_PIN) {
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
        host: Activity,
        type: String,
        playaId: String,
        metadata: Map<String, String>,
        callback: (Intent?) -> Unit
    ) {
        when (type) {
            PATH_ART -> scope.launch(Dispatchers.IO) {
                val item = dataProvider.observeArtByPlayaId(playaId).firstOrNull()
                launch(Dispatchers.Main) {
                    if (item != null) {
                        val intent = IntentUtil.getViewItemDetailIntent(host, item.item as PlayaItem)
                        callback(intent)
                    } else {
                        Timber.w("Object not found: $type/$playaId")
                        callback(null)
                    }
                }
            }
            PATH_CAMP -> scope.launch(Dispatchers.IO) {
                val item = dataProvider.observeCampByPlayaId(playaId).firstOrNull()
                launch(Dispatchers.Main) {
                    if (item != null) {
                        val intent = IntentUtil.getViewItemDetailIntent(host, item.item as PlayaItem)
                        callback(intent)
                    } else {
                        Timber.w("Object not found: $type/$playaId")
                        callback(null)
                    }
                }
            }
            PATH_EVENT -> scope.launch(Dispatchers.IO) {
                try {
                    val item = dataProvider.observeEventByPlayaId(playaId)
                    launch(Dispatchers.Main) {
                        val intent = IntentUtil.getViewItemDetailIntent(host, item.item as PlayaItem)
                        callback(intent)
                    }
                } catch (t: Throwable) {
                    Timber.e(t, "Error loading deep link object: $type/$playaId")
                    launch(Dispatchers.Main) { callback(null) }
                }
            }
            else -> callback(null)
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
        scope.launch(Dispatchers.IO) {
            try {
                db.mapPinDao().insertSuspend(pin)
                launch(Dispatchers.Main) {
                    val intent = Intent(ACTION_SHOW_MAP_PIN).apply {
                        putExtra(EXTRA_PIN_ID, pin.uid)
                        putExtra(EXTRA_PIN_TITLE, pin.title)
                        putExtra(EXTRA_LATITUDE, lat)
                        putExtra(EXTRA_LONGITUDE, lng)
                    }
                    callback(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving map pin")
                launch(Dispatchers.Main) { callback(null) }
            }
        }
    }
    
    private fun isValidBRCCoordinate(lat: Double, lng: Double): Boolean {
        // Black Rock City approximate bounds
        return lat in 40.75..40.82 && lng in -119.25..-119.17
    }
    
    fun dispose() {
        scope.cancel()
    }
}
