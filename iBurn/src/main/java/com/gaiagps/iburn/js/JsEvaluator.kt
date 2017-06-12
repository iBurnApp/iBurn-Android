package com.gaiagps.iburn.js

import android.content.Context
import com.squareup.duktape.Duktape
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

object JsEvaluator {

    val jsPath = "js/bundle.js"
    private var duktape: Duktape? = null
    private var jsContent: String? = null
    private var geocoder: GeoCoder? = null

    internal interface GeoCoder {
        fun reverse(lat: Double, lon: Double): String
        fun forward(playaAddress: String): String
    }

    fun reverseGeocode(context: Context, lat: Float, lon: Float): Single<String> {

        return Single.just(true)
                .observeOn(jsScheduler)
                .map { ignored ->

                    Timber.d("Loading JS...")
                    if (jsContent == null) {
                        val inStream = context.assets.open(jsPath)
                        jsContent = inStream.bufferedReader().use { it.readText() }
                        inStream.close()
                    }
                    Timber.d("Loaded JS")

                    Timber.d("Creating Duktape...")
                    if (duktape == null) {
                        val duktape = Duktape.create()
                        duktape.evaluate("var window = this;" + jsContent + "var coder = window.prepare();")
                        this.duktape = duktape
                    }
                    Timber.d("Created Duktape")

                    Timber.d("Creating Geocoder...")
                    if (geocoder == null) {
                        this.geocoder = duktape?.get("coder", GeoCoder::class.java)
                    }
                    Timber.d("Created Geocoder")

                    Timber.d("Geocoding...")
                    // Call into the JavaScript object to decode a string.
                    val playaAddress = geocoder?.reverse(lat.toDouble(), lon.toDouble())
                    Timber.d("Geocode result %s", playaAddress)
                    playaAddress
                }

    }

    fun close() {
        duktape?.close()
    }
}