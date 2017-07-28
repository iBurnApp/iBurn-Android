package com.gj.animalauto.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
public class BtCar(val device: BluetoothDevice) {

    private val socketScheduler by lazy {
        Schedulers.from(Executors.newSingleThreadExecutor())
    }

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var connectRequested = false
    private var callback: Callback? = null

    fun connect(callback: Callback) {
        this.callback = callback
        connectRequested = true
        val socket = device.createRfcommSocketToServiceRecord(sppUuid)
        this.socket = socket
        manageSocket(socket)
    }

    fun disconnect() {
        Timber.d("Disconnecting from ${device.name}")
        callback = null
        connectRequested = false
        socket?.close()
        socket = null
    }

    private fun manageSocket(socket: BluetoothSocket) {

        val readBuffer = ByteArray(1024)
        val parsingBuffer = ParsingByteBuffer(readBuffer.size * 2)

        var bytesRead = 0

        Observable.just(socket)
                .observeOn(socketScheduler)
                .subscribe {

                    val devName = device.name
                    try {
                        Timber.d("Connecting to $devName...") // Don't change this log without updating reference in LogAnalyzer
                        socket.connect()
                        Timber.d("Connected to $devName!")  // Don't change this log without updating reference in LogAnalyzer
                        callback?.onConnected()

                        val inStream = socket.inputStream

                        while (connectRequested) {
                            Timber.d("Reading from $devName")
                            bytesRead = inStream.read(readBuffer, 0, readBuffer.size)
                            Timber.d("Read $bytesRead bytes from $devName")  // Don't change this log without updating reference in LogAnalyzer

                            parsingBuffer.appendData(readBuffer, 0, bytesRead)
                            val bytesToParse = parsingBuffer.getUndiscardedBytes()
                            val parserResponse = GjMessageFactory.parseAll(bytesToParse, bytesToParse.size)
                            val parsedMessages = parserResponse.messages
                            Timber.d("Parsed ${parsedMessages.size} messages up to byte ${parserResponse.lastParsedRawDataIndex}")  // Don't change this log without updating reference in LogAnalyzer
                            parsedMessages
                                    .filter { it !is GjMessageError }
                                    .forEach {
                                        Timber.d("Parsed message $it from $devName. Callback null: ${this.callback == null}")

                                        this.callback?.let { callback ->
                                            AndroidSchedulers.mainThread().scheduleDirect {
                                                callback.onMessageReceived(it)
                                            }
                                        }
                                    }

                            // Copy data from last parsedByte to bytesRead to head of buffer, then read after
                            val lastParsedByte = parserResponse.lastParsedRawDataIndex
                            parsingBuffer.discardEarliestBytes(lastParsedByte + 1)
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

    // Read 20 bytes

    // Messages occupied 15 bytes

    // response.lastParsedRawDataIndex = 14

    // firstByteOfNextMessage = 15

    // numBytesOfNextMessage = readOffset (0) + bytesRead (20) - firstByteOfNextMessage (15) = 5

    // Copy buffer [15...] -> tmpBuffer [0, 5]
    // Copy tmpBuffer [0...] -> buffer [5]
    // readOffset = 5

}