package com.gj.animalauto.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.GjMessageFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Created by dbro on 7/17/17.
 */
public class BtCar(val device: BluetoothDevice) {

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var connectRequested = false
    private var callback: Callback? = null

    fun connect(callback: Callback) {
        this.callback = this.callback
        connectRequested = true
        val socket = device.createRfcommSocketToServiceRecord(sppUuid)
        this.socket = socket
        manageSocket(socket)
    }

    fun disconnect() {
        callback = null
        connectRequested = false
        socket?.close()
        socket = null
    }

    private fun manageSocket(socket: BluetoothSocket) {

        val buffer = ByteArray(1024)

        Observable.just(socket)
                .observeOn(Schedulers.io())
                .subscribe {

                    val devName = device.name
                    try {
                        Timber.d("Connecting to $devName...")
                        socket.connect()
                        Timber.d("Connected to $devName!")
                        callback?.onConnected()

                        val inStream = socket.inputStream

                        while (connectRequested) {
                            Timber.d("Reading from $devName")
                            val bytesRead = inStream.read(buffer)
                            Timber.d("Read $bytesRead bytes from $devName")
                            val messages = GjMessageFactory.parseAll(buffer)
                            Timber.d("Parsed ${messages.size} messages from $devName")
                            messages.forEach {
                                this.callback?.onMessageReceived(it)
                            }
                        }
                        Timber.d("Closing socket with $devName")
                        socket.close()

                    } catch (e: IOException) {
                        Timber.e(e, "Failed to connect to $devName")
                        callback?.onConnectionFailed(e)
                    }

                }
    }

    override fun toString(): String {
        return "BtCar(device=${device.name})"
    }

    interface Callback {
        fun onConnected()
        fun onConnectionFailed(exception: Exception)
        fun onMessageReceived(message: GjMessage)
    }

}