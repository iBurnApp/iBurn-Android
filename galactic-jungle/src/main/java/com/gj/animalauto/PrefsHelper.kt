package com.gj.animalauto

import android.content.Context
import android.content.SharedPreferences
import java.net.InetAddress

/**
 * Created by dbro on 7/17/17.
 */

class PrefsHelper(context: Context) {

    data class OscHost(val hostname: String, val address: InetAddress, val port: Int)

    private val sharedPrefs: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        editor = sharedPrefs.edit()
    }

    fun getPrimaryCarBtMac(): String? {
        return sharedPrefs.getString(BT_MAC, null)
    }

    fun setPrimaryCarBtMac(macAddress: String) {
        editor.putString(BT_MAC, macAddress).apply()
    }

    fun getPrimaryOscHostname(): String? {
        return sharedPrefs.getString(OSC_HOST, null)
    }

    fun setPrimaryOscHostname(hostname: String) {
        editor.putString(OSC_HOST, hostname).apply()
    }

    fun getOscWifiSsid(): String? {
        return sharedPrefs.getString(OSC_WIFI_SSID, null)
    }

    fun setOscWifiSsid(wifiSsid: String) {
        editor.putString(OSC_WIFI_SSID, wifiSsid).apply()
    }

    fun getOscWifiPass(): String? {
        return sharedPrefs.getString(OSC_WIFI_PASS, null)
    }

    fun setOscWifiPass(wifiPass: String) {
        editor.putString(OSC_WIFI_PASS, wifiPass).apply()
    }

    companion object {

        private val BT_MAC = "bt-mac"   // string
        private val OSC_HOST = "osc-host-name"   // string
        private val OSC_WIFI_SSID = "osc-wifi-ap"   // string
        private val OSC_WIFI_PASS = "osc-wifi-pass"   // string

        private val SHARED_PREFS_NAME = "galactic-prefs"
    }
}