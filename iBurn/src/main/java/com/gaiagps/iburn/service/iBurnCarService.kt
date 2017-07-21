package com.gaiagps.iburn.service

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import com.gaiagps.iburn.BuildConfig
import com.gaiagps.iburn.location.LocationProvider
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.GjMessageGps
import com.gj.animalauto.message.GjMessageStatusResponse
import com.gj.animalauto.service.CarService
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by dbro on 7/19/17.
 */
public class iBurnCarService : CarService() {

    private var mockMessageDisposable: Disposable? = null

    companion object {

        fun startAndBind(context: Context, serviceConnection: ServiceConnection) {
            val carServiceIntent = Intent(context.applicationContext, iBurnCarService::class.java)
            context.startService(carServiceIntent)

            context.bindService(carServiceIntent, serviceConnection, 0) // TODO : Flags?
        }

        fun start(context: Context) {
            val carServiceIntent = Intent(context.applicationContext, iBurnCarService::class.java)
            context.startService(carServiceIntent)
        }

        fun unbind(context: Context, serviceConnection: ServiceConnection) {
            context.unbindService(serviceConnection)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.MOCK) {
            // Don't actually issue any BT connection
            mockGjMessages()
            return START_STICKY
        } else {
            return super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mockMessageDisposable?.dispose()
    }

    private fun mockGjMessages() {
        mockMessageDisposable?.dispose()

        val numVehicles = 5

        val localVehicleId = ((Math.random() * numVehicles - 1) + 1).toByte()

        mockMessageDisposable = Observable.interval(1, TimeUnit.SECONDS)
                .map { tick ->
                    val curVehicleId = (tick % numVehicles) + 1
                    Pair(tick, curVehicleId)
                }
                .subscribe { tickVehicleIdPair ->
                    val tick = tickVehicleIdPair.first.toByte()
                    val vehicleId = tickVehicleIdPair.second.toByte()

                    if (vehicleId == localVehicleId) {
                        // TODO : Send status
                        val data = GjMessageStatusResponse.createData(false, false, false, false, false)
                        val statusMessage = GjMessageStatusResponse(tick, localVehicleId, byteArrayOf(data))
                        onMessageReceived(statusMessage)
                    }

                    val mockLocation = LocationProvider.createMockLocation()
                    val gpsData = GjMessageGps.createData(
                            System.currentTimeMillis().toInt(),
                            mockLocation.latitude,
                            mockLocation.longitude,
                            mockLocation.bearing.toDouble())

                    val message = GjMessageGps(tick, vehicleId, gpsData.array())
                    Timber.d("Delivering mock GjMessageGps for vehicle $vehicleId")
                    onMessageReceived(message)
                }
    }

    override fun onMessageReceived(message: GjMessage) {
        super.onMessageReceived(message)

        if (message is GjMessageGps &&
                message.vehicle == localVehicleId) {

            // This location represents the current device
            Timber.d("Delivering location based on gps message")
            val location = Location("GjLocation")
            location.latitude = message.lat
            location.longitude = message.long
            location.bearing = message.head.toFloat()
            LocationProvider.submitLocation(location)
        }
    }
}