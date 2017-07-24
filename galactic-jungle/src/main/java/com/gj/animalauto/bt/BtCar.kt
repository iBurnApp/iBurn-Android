package com.gj.animalauto.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.GjMessageFactory
import com.gj.animalauto.message.internal.GjMessageError
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
        this.callback = callback
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

        var bytesRead = 0
        var readOffset = 0

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
                            bytesRead = inStream.read(buffer, readOffset, buffer.size - readOffset)
                            Timber.d("Read $bytesRead bytes from $devName starting at $readOffset")

                            val parserResponse = GjMessageFactory.parseAll(buffer, bytesRead)
                            val parsedMessages = parserResponse.messages
                            Timber.d("Parsed ${parsedMessages.size} messages up to byte ${parserResponse.lastParsedRawDataIndex}")
                            parsedMessages
                                    .filter { it !is GjMessageError }
                                    .forEach {
                                        Timber.d("Parsed message $it from $devName. Callback null: ${this.callback == null}")

                                        this.callback?.onMessageReceived(it)
                                    }

                            // Copy data from last parsedByte to bytesRead to head of buffer, then read after
                            val lastParsedByte = parserResponse.lastParsedRawDataIndex
                            val firstByteOfNextMessage = if (lastParsedByte == 0) 0 else lastParsedByte + 1
                            val numBytesOfNextMessage = (readOffset + bytesRead) - lastParsedByte

                            if (numBytesOfNextMessage > 0 && lastParsedByte > 0) {
                                // We have unparsed bytes that need to be prepended to buffer
                                Timber.d("Read $bytesRead bytes, parsed ${parserResponse.lastParsedRawDataIndex}. #bytes of next message (and readOffset):  $numBytesOfNextMessage. firstByteOfNextMessage $firstByteOfNextMessage")
                                // Copy bytes of next message into tmp buffer
                                System.arraycopy(buffer, firstByteOfNextMessage, buffer, 0, numBytesOfNextMessage)
                                readOffset = numBytesOfNextMessage
                            } else {
                                readOffset += bytesRead
                                Timber.d("Incrementing readOffset by $bytesRead. now $readOffset")
                                if (readOffset > buffer.size / 2) {
                                    Timber.d("Resetting read offset")
                                    readOffset = 0
                                }
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

    // Read 20 bytes

    // Messages occupied 15 bytes

    // response.lastParsedRawDataIndex = 14

    // firstByteOfNextMessage = 15

    // numBytesOfNextMessage = readOffset (0) + bytesRead (20) - firstByteOfNextMessage (15) = 5

    // Copy buffer [15...] -> tmpBuffer [0, 5]
    // Copy tmpBuffer [0...] -> buffer [5]
    // readOffset = 5

}