package com.gaiagps.iburn.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

enum class DeepLinkType(val value: String) {
    MapPin("map_pin"),
    PlayaItem("playa_item");
}

fun trackDeepLinkReceived(type: DeepLinkType) {
    // Simple custom event
    val params = Bundle().apply {
        putString("type", type.value)
    }
    Firebase.analytics.logEvent("receive_deep_link", params)
}
