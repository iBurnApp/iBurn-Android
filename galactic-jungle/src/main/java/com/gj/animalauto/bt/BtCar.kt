package com.gj.animalauto.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.gj.animalauto.BuildConfig
import com.gj.animalauto.ParsingByteBuffer
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.GjMessageFactory
import com.gj.animalauto.message.internal.GjMessageError
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

/**
 * Created by dbro on 7/17/17.
 */
const val readBufferSize = 1024

open class BtCar(val device: BluetoothDevice) {

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    protected val socketScheduler by lazy {
        Schedulers.from(Executors.newSingleThreadExecutor())
    }

    val deviceName: String = device.name // This is a somewhat expensive lookup, so we cache it
    private var socket: BluetoothSocket? = null
    private var connectRequested = false
    protected var callback: Callback? = null

    protected val parsingBuffer by lazy {
        ParsingByteBuffer(readBufferSize * 2)
    }

    open fun connect(callback: Callback) {
        this.callback = callback
        connectRequested = true
        val socket = device.createRfcommSocketToServiceRecord(sppUuid)
        this.socket = socket
        manageSocket(socket)
    }

    open fun disconnect() {
        Timber.d("Disconnecting from ${device.name}")
        callback = null
        connectRequested = false
        socket?.close()
        socket = null
    }

    private fun manageSocket(socket: BluetoothSocket) {

        val readBuffer = ByteArray(readBufferSize)

        var bytesRead = 0

        Observable.just(socket)
                .observeOn(socketScheduler)
                .subscribe {

                    try {
                        Timber.d("Connecting to $deviceName...") // Don't change this log without updating reference in LogAnalyzer
                        socket.connect()
                        Timber.d("Connected to $deviceName!")  // Don't change this log without updating reference in LogAnalyzer
                        callback?.onConnected()

                        val inStream = socket.inputStream

                        while (connectRequested) {
                            Timber.d("Reading from $deviceName")
                            bytesRead = inStream.read(readBuffer, 0, readBuffer.size)
                            Timber.d("Read $bytesRead bytes from $deviceName")  // Don't change this log without updating reference in LogAnalyzer

                            onDataReceived(readBuffer, 0, bytesRead)
                        }
                        Timber.d("Closing socket with $deviceName")
                        socket.close()

                    } catch (e: IOException) {
                        Timber.e(e, "Failed to connect to $deviceName")
                        callback?.onConnectionFailed(e)
                    }

                }
    }

    protected fun onDataReceived(data: ByteArray, offset: Int, len: Int) {
        parsingBuffer.appendData(data, offset, len)
        val bytesToParse = parsingBuffer.getUndiscardedBytes()
        val parserResponse = GjMessageFactory.parseAll(bytesToParse, bytesToParse.size)
        val parsedMessages = parserResponse.messages
        Timber.d("Parsed ${parsedMessages.size} messages up to byte ${parserResponse.lastParsedRawDataIndex}")  // Don't change this log without updating reference in LogAnalyzer
        parsedMessages
                .filter { it !is GjMessageError }
                .forEach {
                    Timber.d("Parsed message $it from $deviceName. Callback null: ${this.callback == null}")

                    this.callback?.let { callback ->
                        AndroidSchedulers.mainThread().scheduleDirect {
                            callback.onMessageReceived(it)
                        }
                    }
                }

        val lastParsedByte = parserResponse.lastParsedRawDataIndex
        if (lastParsedByte > 0) {
            parsingBuffer.discardEarliestBytes(lastParsedByte + 1)
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