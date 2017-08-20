package com.gaiagps.iburn

import android.content.Context
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Utility to copy bundled Mapbox offline database so that no internet is required to fetch
 * initial vector map
 * Created by dbro on 8/20/17.
 */

private const val databaseName = "mbgl-offline.db"

fun copyBundledMap(context: Context) {

    Schedulers.io().scheduleDirect {

        val dest = File(context.filesDir, databaseName)

        context.assets
                .open("databases/$databaseName")
                .copyTo(FileOutputStream(dest))
        Timber.d("Copied bundled mapbox map")
    }
}