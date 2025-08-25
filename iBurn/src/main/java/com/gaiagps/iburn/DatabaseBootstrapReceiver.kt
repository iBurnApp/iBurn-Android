package com.gaiagps.iburn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gaiagps.iburn.api.IBurnService
import com.gaiagps.iburn.api.MockIBurnApi
import com.gaiagps.iburn.database.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val provider = DataProvider.getNewInstance(context.applicationContext, dbName)
                val success = IBurnService(context.applicationContext, MockIBurnApi(context.applicationContext)).updateData(provider)
                Timber.d("Bootstrap success: %b", success)
            } catch (t: Throwable) {
                Timber.e(t, "Bootstrap failed")
            } finally {
                pending.finish()
            }
        }
    }
}
