package com.gaiagps.iburn.js

import android.content.Context
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Created by dbro on 6/12/17.
 */

/**
 * Use a single threaded executor to prevent spinning up multiple instances of the JS engine,
 * which is very expensive
 */
private val jsExecutor = Executors.newSingleThreadExecutor()
private val jsDispatcher: CoroutineDispatcher = jsExecutor.asCoroutineDispatcher()

object Geocoder {

    val jsPath = "js/bundle.js"
    private var v8: V8? = null
    private var jsContent: String? = null

    suspend fun reverseGeocode(context: Context, lat: Float, lon: Float): String = withContext(jsDispatcher) {
        init(context)
        Timber.d("Reverse geocoding... $lat / $lon")
        try {
            val playaAddress = v8?.executeStringScript("coder.reverse($lat, $lon)")
            Timber.d("Reverse geocode result %s", playaAddress)
            playaAddress ?: "?"
        } catch (e: Exception) {
            Timber.e(e, "Geocoder exception: $e")
            "?"
        }
    }

    suspend fun forwardGeocode(context: Context, playaAddress: String): LatLng = withContext(jsDispatcher) {
        init(context)
        val result = LatLng()
        if (playaAddress.length < 8) {
            Timber.w("Invalid playa address $playaAddress, not geocoding")
        } else {
            try {
                val latLon = v8?.executeObjectScript("coder.forward(\\\"$playaAddress\\\")")
                latLon?.let {
                    if (it.toString() != "undefined") {
                        val rawCoords = it.getObject("geometry").getObject("coordinates")
                        if (rawCoords is V8Array) {
                            var item: V8Array = rawCoords
                            while (item.type != 2 /* double */) {
                                item = item.getArray(0)
                            }
                            val coords = item.getDoubles(0, 2)
                            result.latitude = coords[1]
                            result.longitude = coords[0]
                        }
                    } else {
                        Timber.w("Undefined result for $playaAddress")
                    }
                }
            } catch (e: Exception) {
                Timber.w("Geocoder exception: $e")
            }
        }
        result
    }

    private fun init(context: Context) {
        if (jsContent == null) {
            Timber.d("Loading JS...")
            val inStream = context.assets.open(jsPath)
            jsContent = inStream.bufferedReader().use { it.readText() }
            inStream.close()
            Timber.d("Loaded JS")
        }

        if (v8 == null) {
            Timber.d("Creating V8...")
            v8 = V8.createV8Runtime()
            Timber.d("Created V8. Initializing Geocoder...")
            v8?.executeVoidScript("var window = this;")
            v8?.executeVoidScript(jsContent)
            v8?.executeVoidScript("var coder = window.prepare();")
            Timber.d("Initialized Geocoder")
        }
    }

    fun close() {
        Timber.d("Closing")
        jsExecutor.execute {
            v8?.release()
            v8 = null
        }
    }
}
