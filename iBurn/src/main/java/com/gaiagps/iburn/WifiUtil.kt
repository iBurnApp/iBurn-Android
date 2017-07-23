package com.gaiagps.iburn

import android.app.Activity
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

/**
 * Created by dbro on 7/23/17.
 */
fun showWifiCredentialsDialog(host: Activity, callback: WifiCredentialCallback) {

    val inflater = host.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

    val layout = inflater.inflate(R.layout.layout_wifi_credentials, null)

    AlertDialog.Builder(host)
            .setTitle("Enter Wifi Credentials")
            .setView(layout)
            .setPositiveButton("Save") { dialogInterface, i ->

                // TODO : Validate before allowing dismiss

                val wifiSsid = (layout.findViewById<View>(R.id.wifi_ssid) as TextView).text.toString()
                val wifiPass = (layout.findViewById<View>(R.id.wifi_pass) as TextView).text.toString()

                callback.onCredentialsEntered(wifiSsid, wifiPass)
            }
            .show()
}

interface WifiCredentialCallback {
    fun onCredentialsEntered(ssid: String, password: String)
}