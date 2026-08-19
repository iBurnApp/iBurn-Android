package com.gaiagps.iburn.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.gaiagps.iburn.BuildConfig
import com.gaiagps.iburn.api.IBurnService
import com.gaiagps.iburn.database.DataProvider
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.TimeUnit


class DataUpdateService(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        fun scheduleAutoUpdate(context: Context) {
            val periodHrs = 24L  // Auto-update should be performed no more than once per 24 hours

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()

            val request = PeriodicWorkRequestBuilder<DataUpdateService>(periodHrs, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            Timber.d("Scheduled auto-update");
        }

        fun updateNow(context: Context) {
            // TODO: Convert to coroutine scope
            // IBurnService(context).updateData() now returns suspend function
            Timber.d("updateNow called - needs coroutine implementation")
        }
    }

    override fun doWork(): Result {
        // A worker scheduled by another installed build can survive an app update.
        // Never let it replace a historical or mock build's bundled records with
        // whatever year the live endpoint currently serves.
        if (!BuildConfig.LIVE_DATA_UPDATES_ENABLED) {
            Timber.d("Skipping live data update for bundled annual data")
            return Result.success()
        }
        val dataProvider = DataProvider.getInstance(applicationContext)
        if (dataProvider.inUpgrade()) return Result.retry()

        val service = IBurnService(applicationContext)
        val success = try {
            runBlocking {
                service.updateData()
            }
        } catch (e: Exception) {
            Timber.e(e, "Update failed")
            false
        }
        Timber.d("Update task finished with success: $success")
        if (!success) {
            return Result.failure()
        }
        return Result.success()
    }
}
