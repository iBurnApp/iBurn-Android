package com.gj.animalauto.bt

import android.bluetooth.BluetoothDevice
import com.gj.animalauto.message.GjMessageGps
import com.gj.animalauto.message.GjMessageStatusResponse
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by dbro on 7/28/17.
 */
public class MockBtCar(device: BluetoothDevice) : BtCar(device) {

    private var nextPacketNum = 0
        get() {
            field += 1
            return field
        }

    private var nextPacketVehicleId = 0
        get() {
            field += 1
            return (field % maxPacketVehicleId) + 1
        }

    private val maxPacketVehicleId = 5
    private val localVehicleId = ((Math.random() * (maxPacketVehicleId - 1)) + 1).toByte()

    private var dataSub : Disposable? = null

    override fun connect(callback: Callback) {
        mockData()
        callback.onConnected()
        this.callback = callback
    }

    override fun disconnect() {
        super.disconnect()
        dataSub?.dispose()
    }

    private fun mockData() {
        Timber.d("mockData")
        dataSub?.dispose()
        dataSub = Observable.interval(2, TimeUnit.SECONDS)
                .observeOn(socketScheduler)
                .subscribe {
                    val statusPacket = mockLocalStatus()
                    Timber.d("Delivering mock status packet")
                    onDataReceived(statusPacket, 0, statusPacket.size)
                    val gpsPacket = mockGps()
                    Timber.d("Delivering mock gps packet")
                    onDataReceived(gpsPacket, 0, gpsPacket.size)
                }
    }

    private fun mockGps() : ByteArray {
        val gpsData = GjMessageGps.createData(
                System.currentTimeMillis().toInt(),
                42.0 + (Math.random() * .05f),
                -122.4 + (Math.random() * .05f),
                (Math.random() * 360f))
        val gpsPacket = GjMessageGps(nextPacketNum.toByte(), nextPacketVehicleId.toByte(), gpsData.array())
        return gpsPacket.toByteArray()
    }

    private fun mockLocalStatus(): ByteArray {
        val statusData = GjMessageStatusResponse.createData(false, false, false, false, false)
        val packet = GjMessageStatusResponse(nextPacketNum.toByte(), localVehicleId, byteArrayOf(statusData))
        return packet.toByteArray()
    }

}