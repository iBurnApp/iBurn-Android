package com.gj.animalauto.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import com.gj.animalauto.CarManager
import com.gj.animalauto.bt.BtCar
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.GjMessageStatusResponse
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by dbro on 7/18/17.
 */

public open class CarService : Service(), BtCar.Callback {

    private val binder = CarBinder()
    private val messageSubject: PublishSubject<GjMessage> = PublishSubject.create()

    private var connectingToPrimaryCar = false

    private val carManager by lazy {
        CarManager(applicationContext)
    }

    protected var localVehicleId : Byte = 0x01

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class CarBinder : Binder() {

        fun observeMessages(): Observable<GjMessage> {
            return messageSubject.hide()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val primaryCar = carManager.getPrimaryBtCar()

        if (primaryCar == null) {
            Timber.e("Primary car not set, stopping service. Did you call PrefsHelper.setPrimaryCarBtMac?")
            stopSelf(startId)
            return START_NOT_STICKY

        } else if (!connectingToPrimaryCar) {
            connectToCar(primaryCar)
        }

        return START_STICKY
    }

    private fun connectToPrimaryCar() {
        val primaryCar = carManager.getPrimaryBtCar()
        primaryCar?.let { primaryCar ->
            connectToCar(primaryCar)
        }
    }

    private fun connectToCar(car: BtCar) {
        connectingToPrimaryCar = true
        car.connect(this)
    }

    // <editor-fold desc="BtCar.Callback">

    override fun onConnected() {

    }

    override fun onConnectionFailed(exception: Exception) {
        connectingToPrimaryCar = false

        // If connection fails, retry connection in a few seconds
        // TODO : Ensure service hasn't stopped in this period?

        Observable.timer(2, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .subscribe {
                    if (!connectingToPrimaryCar) {
                        connectToPrimaryCar()
                    }
                }
    }

    override fun onMessageReceived(message: GjMessage) {

        if (message is GjMessageStatusResponse) {

            // Identifies which vehicle id represents us
            localVehicleId = message.getVehicle()
        }

        messageSubject.onNext(message)
    }

    // </editor-fold desc="BtCar.Callback">

}