package com.gaiagps.iburn.fragment

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.app.Fragment
import com.gaiagps.iburn.service.iBurnCarService
import com.gj.animalauto.message.GjMessage
import com.gj.animalauto.service.CarService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import timber.log.Timber

/**
 * A fragment that should receive [GjMessage] between [onStart] and [onStop]
 * Created by dbro on 7/18/17.
 */
abstract class GjFragment() :Fragment(), ServiceConnection {

    private var messageDisposable: Disposable? = null

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
        unbindService()
        messageDisposable?.dispose()
    }

    private fun bindService() {
        Timber.d("bindService")
        iBurnCarService.startAndBind(activity.applicationContext, this)
    }

    private fun unbindService() {
        Timber.d("unbindService")
        messageDisposable?.dispose()
        iBurnCarService.unbind(activity.applicationContext, this)
    }

    // <editor-fold desc="ServiceConnection">

    override fun onServiceDisconnected(componentName: ComponentName?) {
        Timber.d("onServiceDisconnected")
        messageDisposable?.dispose()
    }

    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
        Timber.d("onServiceConnected")

        binder?.let { binder ->

            val carServiceBinder = binder as CarService.CarBinder

            messageDisposable = carServiceBinder.observeMessages()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { message ->
                        onMessage(message)
                    }
        }
    }

    // </editor-fold desc="ServiceConnection">

    abstract fun onMessage(message: GjMessage)
}