package com.gaiagps.iburn

import android.net.Uri

/**
 * Created by dbro on 8/17/17.
 */
fun isAssetUri(uri: Uri): Boolean {
    return uri.path.startsWith("/android_asset")
}

fun getAssetPathFromAssetUri(uri: Uri): String {
    return uri.path.replace("/android_asset/", "")
}