package com.gaiagps.iburn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gaiagps.iburn.api.IBurnService
import com.gaiagps.iburn.api.MockIBurnApi
import com.gaiagps.iburn.database.DataProvider
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Receiver to bootstrap the playa database from bundled JSON.
 * Trigger with `adb shell am broadcast -a com.gaiagps.iburn.BOOTSTRAP_DB`.
 */
class DatabaseBootstrapReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_BOOTSTRAP_DB = "com.gaiagps.iburn.BOOTSTRAP_DB"
        const val EXTRA_DB_NAME = "com.gaiagps.iburn.EXTRA_DB_NAME"
    }
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val dbName = intent.getStringExtra(EXTRA_DB_NAME) ?: return pending.finish()
        Timber.d("Bootstrapping database %s", dbName)
        DataProvider.getNewInstance(context.applicationContext, dbName)
            .flatMap { provider: DataProvider ->
                IBurnService(context.applicationContext, MockIBurnApi(context.applicationContext))
                    .updateData(provider)
                    .toObservable()
            }
            .subscribeOn(Schedulers.io())
            .subscribe({ success: Boolean ->
                Timber.d("Bootstrap success: %b", success)
                pending.finish()
            }, { error: Throwable ->
                Timber.e(error, "Bootstrap failed")
                pending.finish()
            })
    }
}
