package com.gaiagps.iburn

import android.content.Context
import android.widget.ImageView
import com.gaiagps.iburn.database.Art
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception

/**
 * Created by dbro on 8/16/17.
 */

private val httpClient by lazy { OkHttpClient() }

private const val useBundledArtImages = true

fun getArtImageLocalPath(context: Context, art: Art): String? {
    if (useBundledArtImages) {
        return getArtImageAssetPath(art)
    } else {
        val cachedArt = getCachedArtImageFile(context, art.imageUrl)
        if (!cachedArt.exists()) return null

        return cachedArt.absolutePath
    }
}

fun loadArtImage(art: Art, view: ImageView, callback: Callback? = null) {

    if (!art.hasImage()) {
        callback?.onError()
        return
    }

    val context = view.context.applicationContext

    if (useBundledArtImages) {
        val assetPath = getArtImageAssetPath(art)

        val picasso = Picasso.get()
        picasso
                .load(assetPath)
                .into(view, object : com.squareup.picasso.Callback {

                    override fun onSuccess() {
                        callback?.onSuccess()
                    }

                    override fun onError(e: Exception?) {
                        callback?.onError()
                    }
                })
    } else {
        // Load and cache art images from Internet
        val cachedFile = getCachedArtImageFile(context, art.imageUrl)

        val picasso = Picasso.get()
        if (cachedFile.exists()) {
            Timber.d("Cache hit for ${art.name} image")
            picasso
                    .load(cachedFile)
                    .into(view, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            callback?.onSuccess()
                        }

                        override fun onError(e: Exception?) {
                            callback?.onError()
                        }
                    })
        } else {
            Timber.d("Cache miss for ${art.name} image")
            cacheArtImageFile(context, art, object : Callback {
                override fun onSuccess() {
                    loadArtImage(art, view, callback)
                }

                override fun onError() {
                    callback?.onError()
                }

            })
        }
    }
}

private fun cacheArtImageFile(context: Context, art: Art, callback: Callback) {
    val imageUrl = art.imageUrl
    val request = Request.Builder()
            .url(imageUrl)
            .build()

    Timber.d("Downloading image for ${art.name}")
    httpClient.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onResponse(call: Call?, response: Response?) {
            if (response?.isSuccessful == true) {
                Timber.d("Downloaded image for ${art.name}")
                val destFile = getCachedArtImageFile(context, imageUrl)
                val outStream = FileOutputStream(destFile)
                val inStream = response?.body()?.byteStream()
                if (inStream != null) {
                    inStream.copyTo(outStream)
                    postResult(callback)
                } else {
                    postResult(callback, succcess = false)
                }
            } else {
                Timber.d("Download failed")
                postResult(callback, succcess = false)
            }
        }

        override fun onFailure(call: Call?, e: IOException?) {
            Timber.e(e, "Download failed")
            postResult(callback, succcess = false)
        }

    })
}

private fun postResult(callback: Callback, succcess: Boolean = true) {
    AndroidSchedulers.mainThread().scheduleDirect {
        if (succcess) callback.onSuccess() else callback.onError()
    }
}

/**
 * If caching art images from remote url serving
 */
private fun getCachedArtImageFile(context: Context, mediaPath: String): File {
    Timber.d("Getting cached art for $mediaPath")
    return File(getArtImagesDirectory(context), mediaPath.substring(mediaPath.lastIndexOf(File.separator), mediaPath.length))
}

/**
 * If pulling art images directly from bundled app assets
 */
private fun getArtImageAssetPath(art: Art): String {
    Timber.d("Getting bundled art for ${art.name}")
    return "file:///android_asset/art_images/${art.playaId}.webp"
}

private fun getArtImagesDirectory(context: Context): File {
    val dir = File(context.filesDir, "art_images")
    dir.mkdirs()
    return dir
}

interface Callback {
    fun onSuccess()
    fun onError()
}