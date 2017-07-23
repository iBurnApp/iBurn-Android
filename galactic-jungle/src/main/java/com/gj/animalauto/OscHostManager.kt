package com.gj.animalauto

import android.app.Activity
import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.net.InetAddress

/**
 * Manages discovering and associating with OSC hosts over WiFi
 * Created by dbro on 7/23/17.
 */
public class OscHostManager(val context: Context) : OscMdnsManager.Callback {
    data class OscHost(val hostname: String, val address: InetAddress, val port: Int)

    private val discoveredOscHosts = ArrayList<OscHost>()
    private val discoveredOscHostsAdapter = OscHostAdapter(context, discoveredOscHosts)

    private var selectedHostCallback: ((OscHost) -> Unit)? = null

    var isShowingDialog = false
        private set

    private var selectingNewHost = false

    private val gjPrefs by lazy {
        PrefsHelper(context.applicationContext)
    }

    private val oscMdns by lazy {
        OscMdnsManager(context.applicationContext, OscClient.defaultLocalPort)
    }

    fun getPrimaryOscHostname(): String? {
        return gjPrefs.getPrimaryOscHostname()
    }

    fun setPrimaryOscHost(hostName: String) {
        gjPrefs.setPrimaryOscHostname(hostName)
    }

    fun startDiscoveryOfNewHost(hostActivity: Activity, callback: (OscHost) -> Unit) {
        selectingNewHost = true
        startDiscovery(hostActivity, callback)
    }

    fun startDiscovery(hostActivity: Activity, callback: (OscHost) -> Unit) {
        Timber.d("Starting discovery")
        // Show dialog if OSC host selection is necessary
        if (getPrimaryOscHostname() == null || selectingNewHost) {
            showDiscoveryDialog(hostActivity, callback)
        }

        this.selectedHostCallback = callback

        oscMdns.callback = this

        // TODO : Separate call to advertise our own OSC service?
        //oscMdns.registerService()
        oscMdns.discoverPeers()
    }

    fun stopDiscovery() {
        oscMdns.release()
    }

    fun showDiscoveryDialog(hostActivity: Activity, callback: (OscHost) -> Unit) {
        Timber.d("Showing discovery dialog")
        this.selectedHostCallback = callback
        val dialog = createDialog(hostActivity)
        dialog.show()
        isShowingDialog = true
    }

    fun onHostDiscovered(host: OscHost) {
        AndroidSchedulers.mainThread().scheduleDirect {
            Timber.d("Discovered host ${host.hostname}")
            if (getPrimaryOscHostname() == null || selectingNewHost) {
                Timber.d("Adding host ${host.hostname} to adapter list")
                discoveredOscHostsAdapter.add(host)
            } else if (getPrimaryOscHostname().equals(host.hostname)) {
                Timber.d("Notifingn client that selected host ${host.hostname} is discovered")
                selectedHostCallback?.invoke(host)
            } else {
                Timber.w("Discovered host ${host.hostname} does not match primary ${getPrimaryOscHostname()}")
            }
        }
    }

    private fun createDialog(hostActivity: Activity): AlertDialog {
        val builder = AlertDialog.Builder(hostActivity)
        builder.setTitle("Select OSC Host")
                .setAdapter(discoveredOscHostsAdapter, {
                    _, position ->
                    val selectedHost = discoveredOscHosts[position]
                    Timber.d("User selected OSC host ${selectedHost.hostname}")
                    selectedHostCallback?.invoke(selectedHost)
                })

        val dialog = builder.create()

        // Clients should call stopDiscovery, but to be safe let's also stop
        // when the dialog is dismissed
        dialog.setOnDismissListener {
            isShowingDialog = false
        }

        return builder.create()
    }


    class OscHostAdapter(ctx: Context, cars: List<OscHost>) : ArrayAdapter<OscHost>(ctx, android.R.layout.simple_list_item_1, cars) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val host = getItem(position)
            // Check if an existing view is being reused, otherwise inflate the view
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.simple_list_item, parent, false)
            }
            val label = if (TextUtils.isEmpty(host.hostname)) "${host.address}:${host.port}" else "${host.hostname} - ${host.address}:${host.port}"
            (view as TextView).text = label
            return view
        }
    }

    // OscMdns callback

    override fun onPeerDiscovered(hostName: String, hostAddress: InetAddress, hostPort: Int) {
        onHostDiscovered(OscHost(hostName, hostAddress, hostPort))
    }

}