package com.gaiagps.iburn.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.gaiagps.iburn.R
import com.gaiagps.iburn.WifiCredentialCallback
import com.gaiagps.iburn.ioScheduler
import com.gaiagps.iburn.showWifiCredentialsDialog
import com.gj.animalauto.CarManager
import com.gj.animalauto.OscHostManager
import com.gj.animalauto.PrefsHelper
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.MessageAdapter
import com.gj.animalauto.message.MessageLog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by dbro on 7/23/17.
 */
public class GjSettingsFragment : GjFragment() {

    val oscHostItem: View  by lazy {
        val view: View = view!!.findViewById(R.id.paired_osc_host_item)
        view
    }

    val oscHostItemValue: TextView by lazy {
        val view: TextView = view!!.findViewById(R.id.primary_osc_hostname)
        view
    }

    val btItem: View by lazy {
        val view: View = view!!.findViewById(R.id.paired_bt_device_item)
        view
    }

    val btItemValue: TextView by lazy {
        val view: TextView = view!!.findViewById(R.id.primary_bt_mac_addr)
        view
    }

    val oscItem: View by lazy {
        val view: View = view!!.findViewById(R.id.osc_wifi_item)
        view
    }

    val oscItemValue: TextView by lazy {
        val view: TextView = view!!.findViewById(R.id.osc_wifi_credentials)
        view
    }

    val messageConsoleValue: ListView by lazy {
        val view: ListView = view!!.findViewById(R.id.console_item_value)
        view
    }

    val carManager by lazy {
        CarManager(context.applicationContext)
    }

    val oscManager by lazy {
        OscHostManager(context.applicationContext)
    }

    val gjPrefs by lazy {
        PrefsHelper(context.applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        inflater?.let { inflater ->
            val view = inflater.inflate(R.layout.fragment_gj_settings, container, false)

            return view
        }

        return null
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        updateItemValueViews()

        oscHostItem.setOnClickListener {
            discoverOscHosts()
        }

        btItem.setOnClickListener {
            discoverBtDevices()
        }

        oscItem.setOnClickListener {
            showWifiCredentialsDialog(activity, object : WifiCredentialCallback {
                override fun onCredentialsEntered(ssid: String, password: String) {
                    gjPrefs.setOscWifiSsid(ssid)
                    gjPrefs.setOscWifiPass(password)
                }

            })
        }

        messageConsoleValue.adapter = messageAdapter

    }

    private val maxMessageAdapterSize = 1000
    private val messageAdapter by lazy {
        MessageAdapter(context, ArrayList())
    }

    override fun onMessage(message: GjMessage) {

        if (messageAdapter.count == maxMessageAdapterSize) {
            messageAdapter.clear()
        }

        // If the listView was scrolled to the bottom, keep it there
        val wasAtLastPosition = messageConsoleValue.lastVisiblePosition == messageAdapter.count - 1
        Timber.d("ListView is at position ${messageConsoleValue.lastVisiblePosition}. ${messageAdapter.count} total items. ")
        messageAdapter.add(MessageLog(message = message, time = System.currentTimeMillis()))

        if (wasAtLastPosition) {
            messageConsoleValue.setSelection(messageAdapter.count)
        }
    }

    override fun onStop() {
        super.onStop()

        carManager.stopDiscovery()
        oscManager.stopDiscovery()
    }

    private fun updateItemValueViews() {
        oscHostItemValue.text = gjPrefs.getPrimaryOscHostname() ?: "None"
        btItemValue.text = gjPrefs.getPrimaryCarBtMac() ?: "None"

        if (!TextUtils.isEmpty(gjPrefs.getOscWifiSsid())) {
            oscItemValue.text = "SSID: " + gjPrefs.getOscWifiSsid() + " WPA2 Password: " + gjPrefs.getOscWifiPass()
        } else {
            oscItemValue.text = "None"
        }
    }

    private fun discoverBtDevices() {
        carManager.startDiscovery(activity, { selectedBtCar ->

            Timber.d("User selected car %s. Saving as primary and connecting...", selectedBtCar)
            carManager.setPrimaryBtCar(selectedBtCar)
            updateItemValueViews()
        })
    }

    private fun discoverOscHosts() {
        oscManager.startDiscoveryOfNewHost(activity, { selectedHost ->
            oscManager.setPrimaryOscHost(selectedHost.hostname)
            updateItemValueViews()
        })
    }
}