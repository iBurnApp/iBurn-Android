package com.gj.animalauto

import com.illposed.osc.*
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.Executors


/**
 * Created by dbro on 7/21/17.
 */
public class OscClient(val hostAddress: InetAddress = InetAddress.getByName("192.168.0.100"), val hostPort: Int = 8000) {

    companion object {
        val defaultLocalPort = 9000
    }

    private val commandScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    private var oscOut = OSCPortOut(hostAddress, hostPort)
    private var oscIn = OSCPortIn(defaultLocalPort)

    private val addressFader = "/GLOBAL/fader"

    private val brightnessFader = "2"
    private val saturationFader = "4"
    private val densityFader = "8"
    private val speedFader = "7"

    private val addressPushButton = "/GLOBAL/push"
    private val pushButtonRange = IntRange(1, 10)

    init {
        Timber.d("Opening OSC Sockets")
    }


    fun setBrightness(brightness: Float) {
        Timber.d("Send brightness $brightness")
        sendCommand(addressFader + brightnessFader, floatArrayOf(brightness).toList())
    }

    fun setSaturation(saturation: Float) {
        Timber.d("Send saturation $saturation")
        sendCommand(addressFader + saturationFader, floatArrayOf(saturation).toList())
    }

    fun setDensity(density: Float) {
        Timber.d("Send density $density")
        sendCommand(addressFader + densityFader, floatArrayOf(density).toList())
    }

    fun setSpeed(speed: Float) {
        Timber.d("Send speed $speed")
        sendCommand(addressFader + speedFader, floatArrayOf(speed).toList())
    }

    fun sendButtonPress(button: Int) {
        if (!pushButtonRange.contains(button)) {
            Timber.w("Button $button is outside known range $pushButtonRange")
        }
        sendCommand(addressPushButton + button.toString(), floatArrayOf(1f).toList())
    }

    private fun sendCommand(address: String, args: Collection<Any>) {
        commandScheduler.scheduleDirect {
            val message = OSCMessage(address, args)
            try {
                oscOut.send(message)
                Timber.d("Send message to $address at $hostAddress:$hostPort")
            } catch (e: SocketException) {
                Timber.e(e, "Error sending command to address $address")
            }
        }
    }

    fun listen() {
        oscIn.addListener("/", { time, message ->
            if (message != null) {
                Timber.d("Received message $message")
            }
        })
        oscIn.startListening()
    }

    fun release() {
        Timber.d("Closing OSC Sockets")
        oscIn.stopListening()
        oscIn.close()
        oscOut.close()
    }
}