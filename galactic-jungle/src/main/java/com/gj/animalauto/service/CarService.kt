package com.gj.animalauto.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.gj.animalauto.CarManager
import com.gj.animalauto.bt.BtCar
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.message.GjMessageStatusResponse
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by dbro on 7/18/17.
 */

const val connectionFailuresPerBtReset = 3

public open class CarService : Service(), BtCar.Callback {

    private val connnectionRetryScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    private val binder = CarBinder()
    private val messageSubject: PublishSubject<GjMessage> = PublishSubject.create()

    private var car: BtCar? = null
    private var reconnectDisposable: Disposable? = null
    private var btConnectionErrorCount = 0

    private var connectingOrConnectedToCar = false

    private val carManager by lazy {
        CarManager(applicationContext)
    }

    protected var localVehicleId : Byte = 0xFF.toByte()

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

        } else {

            // If we're connected but to a car different from the new primary, disconnect
            if (connectingOrConnectedToCar && car?.device?.address != primaryCar.device.address) {
                car?.disconnect()
                connectingOrConnectedToCar = false
            }

            if (!connectingOrConnectedToCar) {
                connectToCar(primaryCar)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        connectingOrConnectedToCar = false
        reconnectDisposable?.dispose()
        car?.disconnect()
    }

    private fun connectToPrimaryCar() {
        val primaryCar = carManager.getPrimaryBtCar()
        primaryCar?.let { primaryCar ->
            connectToCar(primaryCar)
        }
        if (primaryCar == null) {
            Timber.w("connectToPrimaryCar called with no primary car set. Ignoring")
        }
    }

    private fun connectToCar(car: BtCar) {
        this.car = car
        connectingOrConnectedToCar = true
        car.connect(this)
    }

    // <editor-fold desc="BtCar.Callback">

    override fun onConnected() {
        Timber.d("Car connection succesful! Resetting connn error count")
        reconnectDisposable?.dispose()
        btConnectionErrorCount = 0
    }

    override fun onConnectionFailed(exception: Exception) {
        connectingOrConnectedToCar = false
        btConnectionErrorCount++

        Timber.w("onConnectionFailed with error count $btConnectionErrorCount. Retrying...")

        // If connection fails, retry connection in a few seconds
        // if btConnectionErrorCount is high, also power toggle BT
        reconnectDisposable = Observable.timer(2, TimeUnit.SECONDS)
                .observeOn(connnectionRetryScheduler)
                .skipWhile { connectingOrConnectedToCar }
                .flatMap { tick ->

                    // Force a disconnect before proceeding with re-connect if possible

                    this.car?.let { car ->
                        car.disconnect()
                        Observable.timer(2, TimeUnit.SECONDS)
                    }
                    Observable.just(tick)
                }
                .flatMap { tick ->
                    if (btConnectionErrorCount > connectionFailuresPerBtReset) {

                        carManager.resetBluetooth()
                    } else {
                        Observable.just(tick)
                    }
                }
                .subscribe {
                    if (!connectingOrConnectedToCar) {
                        // NOTE: If we exposed a public method to connect to arbitrary
                        // cars, it wouldn't be correct to assume re-connection to primary car
                        Timber.d("onConnectionFailed : re-issuing connection to primary car")
                        connectToPrimaryCar()
                    }
                }
    }

    override fun onMessageReceived(message: GjMessage) {
        Timber.d("CarService got message $message")
        if (message is GjMessageStatusResponse) {

            // Identifies which vehicle id represents us
            localVehicleId = message.getVehicle()
        }

        messageSubject.onNext(message)
    }

    // </editor-fold desc="BtCar.Callback">

}