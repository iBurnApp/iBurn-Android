package com.gaiagps.iburn.js

import android.content.Context
import android.widget.Toast
import com.gaiagps.iburn.BuildConfig
import com.squareup.duktape.Duktape
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
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

                    if (jsContent == null) {
                        Timber.d("Loading JS...")
                        val inStream = context.assets.open(jsPath)
                        jsContent = inStream.bufferedReader().use { it.readText() }
                        inStream.close()
                        Timber.d("Loaded JS")
                    }

                    if (duktape == null) {
                        val startTime = System.currentTimeMillis()
                        Timber.d("Creating Duktape...")
                        val duktape = Duktape.create()
                        duktape.evaluate("var window = this;" + jsContent + "var coder = window.prepare();")
                        this.duktape = duktape
                        val logStr = "Created Duktape in ${(System.currentTimeMillis() - startTime) / 1000f} s"
                        Timber.d(logStr)
                        if (BuildConfig.DEBUG) {
                            AndroidSchedulers.mainThread().scheduleDirect {
                                Toast.makeText(context, logStr, Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    if (geocoder == null) {
                        Timber.d("Creating Geocoder...")
                        this.geocoder = duktape?.get("coder", GeoCoder::class.java)
                        Timber.d("Created Geocoder")
                    }

                    Timber.d("Geocoding...")
                    // Call into the JavaScript object to decode a string.
                    val playaAddress = geocoder?.reverse(lat.toDouble(), lon.toDouble())
                    Timber.d("Geocode result %s", playaAddress)
                    playaAddress
                }

    }

    fun close() {
        Timber.d("Closing")
        duktape?.close()
        duktape = null
    }
}