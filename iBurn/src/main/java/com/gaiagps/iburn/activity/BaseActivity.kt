package com.gaiagps.iburn.activity

import android.support.v7.app.AppCompatActivity
import com.gaiagps.iburn.log.DiskLogger
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by dbro on 7/27/17.
 */
open class BaseActivity : AppCompatActivity() {

    companion object {
        val numStartedActivities = AtomicInteger(0)
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart %d", numStartedActivities.incrementAndGet())
        if (numStartedActivities.get() == 1) {
            DiskLogger.getSharedInstance(applicationContext).startSession()
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop %d", numStartedActivities.decrementAndGet())
        if (numStartedActivities.get() == 0) {
            DiskLogger.getSharedInstance(applicationContext).stopSession()
        }
    }

}