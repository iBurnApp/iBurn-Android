package com.gj.animalauto

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import com.gj.animalauto.bt.BtCar
import com.gj.animalauto.bt.BtManager
import timber.log.Timber


/**
 * Created by dbro on 7/14/17.
 */
public class CarManager(val context: Context) {

    val btManager = BtManager(context)

    private val discoveredCars = ArrayList<BluetoothDevice>()
    private val discoveredCarsAdapter = ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_1, discoveredCars)

    private var discoveryCallback: ((BtCar) -> Unit)? = null

    /**
     * Start car (Really just BT device) discovery, displaying a dialog reporting the search results.
     * Callback reports when user selects a discovered device.
     * Call [stopDiscovery] when discovery is no longer desired.
     */
    fun startDiscovery(hostActivity: Activity, callback: (BtCar) -> Unit) {

        discoveryCallback = callback
        discoveredCarsAdapter.clear()

        btManager.startDiscovery {
            device ->
            discoveredCarsAdapter.add(device)
        }

        val dialog = createDialog(hostActivity)
        dialog.show()
    }

    fun stopDiscovery() {
        discoveryCallback = null
        btManager.stopDiscovery()
    }

    private fun createDialog(hostActivity: Activity): AlertDialog {
        val builder = AlertDialog.Builder(hostActivity)
        builder.setTitle("Select Car")
                .setAdapter(discoveredCarsAdapter, {
                    dialogInterface, position ->
                    val selectedDevice = discoveredCars[position]
                    Timber.d("User selected car ${selectedDevice.name}")
                    discoveryCallback?.invoke(BtCar(selectedDevice))
                })

        val dialog = builder.create()

        // Clients should call stopDiscovery, but to be safe let's also stop
        // when the dialog is dismissed
        dialog.setOnDismissListener {
            stopDiscovery()
        }

        return builder.create()
    }
}