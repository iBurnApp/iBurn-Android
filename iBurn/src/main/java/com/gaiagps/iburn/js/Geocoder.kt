package com.gaiagps.iburn.js

import android.content.Context
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Created by dbro on 6/12/17.
 */

/**
 * Use a single threaded executor to prevent spinning up multiple instances of the JS engine,
 * which is very expensive
 */
private val jsScheduler =
        Schedulers.from(
                Executors.newSingleThreadExecutor()
        )

object Geocoder {

    val jsPath = "js/bundle.js"
    private var v8: V8? = null
    private var jsContent: String? = null

    fun reverseGeocode(context: Context, lat: Float, lon: Float): Single<String> {

        return Single.just(true)
                .observeOn(jsScheduler)
                .map { ignored ->

                    init(context)

                    Timber.d("Reverse geocoding...")
                    // Call into the JavaScript object to decode a string.
                    val playaAddress = v8?.executeStringScript("coder.reverse($lat, $lon)")
                    Timber.d("Reverse geocode result %s", playaAddress)

                    playaAddress ?: "?"
                }

    }

    fun forwardGeocode(context: Context, playaAddress: String): Single<LatLng> {
        return Single.just(true)
                .observeOn(jsScheduler)
                .map { ignored ->

                    init(context)

                    val result = LatLng()
                    Timber.d("Forward geocoding '$playaAddress'...")
                    if (playaAddress.length < 8) {
                        Timber.w("Invalid playa address $playaAddress, not geocoding")
                    } else {
                        // Call into the JavaScript object to decode a string.
                        val latLon = v8?.executeObjectScript("coder.forward(\"$playaAddress\")")
                        Timber.d("Forward geocode result %s", latLon)

                        latLon?.let {
                            if (it.toString() == "undefined") {
                                Timber.w("Undefined result for $playaAddress")
                                return@let
                            }
                            val rawCoords = it.getObject("geometry").getObject("coordinates")
                            if (rawCoords is V8Array) {
                                var item: V8Array = rawCoords
                                while (item.type != 2 /* double */) {
                                    item = item.getArray(0)
                                }
                                val coords = item.getDoubles(0, 2)
                                Timber.d("Got coords! ${coords[0]}, ${coords[1]}")
                                result.latitude = coords[1]
                                result.longitude = coords[0]
                            }
                        }
                    }
                    result
                }
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
        jsScheduler.scheduleDirect {
            v8?.release()
            v8 = null
        }
    }
}