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
import timber.log.Timber
import java.net.InetAddress

/**
 * Manages discovering and associating with OSC hosts over WiFi
 * Created by dbro on 7/23/17.
 */
public class OscHostDiscoveryDialog(val context: Context) {

    data class OscHost(val hostname: String, val address: InetAddress, val port: Int)

    private val discoveredOscHosts = ArrayList<OscHost>()
    private val discoveredOscHostsAdapter = OscHostAdapter(context, discoveredOscHosts)

    private var selectedHostCallback: ((OscHost) -> Unit)? = null

    var isShowingDialog = false
        private set

    fun showDiscoveryDialog(hostActivity: Activity, callback: (OscHost) -> Unit) {
        this.selectedHostCallback = callback
        val dialog = createDialog(hostActivity)
        dialog.show()
        isShowingDialog = true
    }

    fun onHostDiscovered(host: OscHost) {
        discoveredOscHostsAdapter.add(host)
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


    class OscHostAdapter(ctx: Context, cars: List<OscHost>): ArrayAdapter<OscHost>(ctx, android.R.layout.simple_list_item_1, cars) {

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

}