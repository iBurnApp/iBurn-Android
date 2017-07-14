package com.gaiagps.iburn.js

import android.content.Context
import com.eclipsesource.v8.V8
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

                    Timber.d("Geocoding...")
                    // Call into the JavaScript object to decode a string.
                    val playaAddress = v8?.executeStringScript("coder.reverse($lat, $lon)")
                    Timber.d("Geocode result %s", playaAddress)

                    playaAddress ?: "?"
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