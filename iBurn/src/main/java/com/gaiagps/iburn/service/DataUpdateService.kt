package com.gaiagps.iburn.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
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