package com.gj.animalauto

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import timber.log.Timber
import java.net.InetAddress


/**
 * Created by dbro on 7/22/17.
 */
public class OscMdnsManager(val context: Context, val localPort: Int) {

    interface Callback {
        fun onPeerDiscovered(hostName: String, hostAddress: InetAddress, hostPort: Int)
    }

    private val serviceTypeOsc = "_osc._udp."
    private var serviceName = "gj-tablet"

    private var nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val registrationListener = RegistrationListener()
    private val discoveryListener = DiscoveryListener()
    private val resolveListener = ResolveListener()

    var callback: Callback? = null

    fun discoverPeers() {
        Timber.d("Starting peer discovery")
        nsdManager.discoverServices(
                serviceTypeOsc, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun registerService() {

        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo()

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.serviceName = "gj-tablet"
        serviceInfo.serviceType = serviceTypeOsc
        serviceInfo.port = localPort

        Timber.d("Registering service")
        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun release() {
        Timber.d("Unregistering service and stopping discovery")

        try {
            nsdManager.unregisterService(registrationListener)
        } catch (e: IllegalArgumentException) {
            // registrationListener not registered
        }

        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: IllegalArgumentException) {
            // discoveryListener not registered
        }
    }

    // NSD Service Registration Callbacks:

    inner class RegistrationListener : NsdManager.RegistrationListener {
        override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {
            Timber.w("onUnregistrationFailed")
        }

        override fun onServiceUnregistered(p0: NsdServiceInfo?) {
            Timber.w("onServiceUnregistered")
        }

        override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {
            Timber.w("onRegistrationFailed")
        }

        override fun onServiceRegistered(service: NsdServiceInfo?) {
            Timber.d("onServiceRegistered")
            service?.let { service ->
                serviceName = service.serviceName
            }
        }
    }

    // NSD Service Discovery Callbacks:

    inner class DiscoveryListener : NsdManager.DiscoveryListener {
        override fun onServiceFound(service: NsdServiceInfo?) {
            Timber.d("onServiceFound")
            service?.let { service ->
                if (service.serviceType != serviceTypeOsc) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Timber.w("Found service of unknown type: " + service.serviceType)
                } else if (service.serviceName == serviceName) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Timber.d("Discovered ourselves... Guess that's good")
                } else {
                    Timber.d("Discovered another OSC peer with name ${service.serviceName}. Resolving service info...")
                    nsdManager.resolveService(service, resolveListener)
                }
            }
        }

        override fun onStopDiscoveryFailed(p0: String?, p1: Int) {
            Timber.w("onStopDiscoveryFailed")
        }

        override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
            Timber.w("onStartDiscoveryFailed")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onDiscoveryStarted(p0: String?) {
            Timber.w("onDiscoveryStarted")
        }

        override fun onDiscoveryStopped(p0: String?) {
            Timber.w("onDiscoveryStopped")
        }

        override fun onServiceLost(p0: NsdServiceInfo?) {
            Timber.w("onServiceLost")
        }
    }

    // NSD Service Resolution Callbacks

    inner class ResolveListener : NsdManager.ResolveListener {

        override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
            Timber.w("onResolveFailed")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            Timber.d("onServiceResolved")
            serviceInfo?.let { serviceInfo ->
                if (serviceInfo.serviceName == serviceName) {
                    // We resolved ourself. D-oh!
                    Timber.w("Resolved local host. D-oh!")
                    return
                }
                callback?.onPeerDiscovered(serviceInfo.serviceName, serviceInfo.host, serviceInfo.port)
            }
        }

    }
}