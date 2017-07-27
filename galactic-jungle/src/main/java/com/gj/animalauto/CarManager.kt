package com.gj.animalauto

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.gj.animalauto.bt.BtCar
import com.gj.animalauto.bt.BtManager
import timber.log.Timber
import android.view.LayoutInflater
import android.widget.TextView
import io.reactivex.Observable


/**
 * Manages discovering and associating with Gj main board over BT
 * Created by dbro on 7/14/17.
 */
public class CarManager(val context: Context) {

    val btManager = BtManager(context)

    private val discoveredCars = ArrayList<BluetoothDevice>()
    private val discoveredCarsAdapter = CarBluetoothDeviceAdapter(context, discoveredCars)

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

    /**
     * We all love BT
     */
    fun resetBluetooth() : Observable<Boolean> {
        return btManager.reset()
    }

    fun getPrimaryBtCar(): BtCar? {
        val prefs = PrefsHelper(context.applicationContext)
        val primaryBtleMac = prefs.getPrimaryCarBtMac()
        if (primaryBtleMac != null) {
            val device = btManager.getDeviceWithMac(primaryBtleMac)
            return BtCar(device)
        }
        return null
    }

    fun setPrimaryBtCar(car: BtCar) {
        val prefs = PrefsHelper(context.applicationContext)
        prefs.setPrimaryCarBtMac(car.device.address)
    }

    private fun createDialog(hostActivity: Activity): AlertDialog {
        val builder = AlertDialog.Builder(hostActivity)
        builder.setTitle("Select Car's Bluetooth Adapter")
                .setAdapter(discoveredCarsAdapter, {
                    _, position ->
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

    class CarBluetoothDeviceAdapter(ctx: Context, cars: List<BluetoothDevice>): ArrayAdapter<BluetoothDevice>(ctx, android.R.layout.simple_list_item_1, cars) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val device = getItem(position)
            // Check if an existing view is being reused, otherwise inflate the view
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.simple_list_item, parent, false)
            }
            val label = if (TextUtils.isEmpty(device.name)) device.address else "${device.name} - ${device.address}"
            (view as TextView).text = label
            return view
        }
    }
}