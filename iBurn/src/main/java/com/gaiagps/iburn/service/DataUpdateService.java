package com.gaiagps.iburn.service;

import android.content.Context;
import android.transition.Explode;

import com.gaiagps.iburn.api.IBurnService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import net.hockeyapp.android.ExceptionHandler;

import timber.log.Timber;

/**
 * This service gets awoken by the Google Cloud Messaging Network Manager
 * and allows us to perform our update task at opportune times.
 * <p>
 * Created by davidbrodsky on 7/2/15.
 */
public class DataUpdateService extends GcmTaskService {

    public static final String AUTO_UPDATE_TASK_NAME = "iburn-auto-update";

    public static void scheduleAutoUpdate(Context context) {
        long periodSecs = 60 * 60 * 24;     // Auto-update should be performed no more than once per 24 hours

        PeriodicTask dailyUpdate = new PeriodicTask.Builder()
                .setService(DataUpdateService.class)
                .setPeriod(periodSecs)
                .setTag(AUTO_UPDATE_TASK_NAME)
                .setPersisted(true)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .setUpdateCurrent(true)
                .build();

        GcmNetworkManager.getInstance(context).schedule(dailyUpdate);
        Timber.d("Scheduled auto-update");
    }

    /**
     * Called on application or Google Play Services update
     */
    @Override
    public void onInitializeTasks() {
        scheduleAutoUpdate(getApplicationContext());
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        try {
            if (taskParams.getTag().equals(AUTO_UPDATE_TASK_NAME)) {
                Timber.d("GCM invoked update task");
                IBurnService service = new IBurnService(getApplicationContext());
                boolean success = service.updateData().singleOrDefault(true).toBlocking().single();
                Timber.d("GCM invoked task finished with success %b", success);
                return success ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_RESCHEDULE;
            }
            Timber.w("Unknown task (%s) invoked", taskParams.getTag());
            return GcmNetworkManager.RESULT_FAILURE;
        } catch (Exception e) {
            ExceptionHandler.saveException(e, null);
            Timber.e(e, "GCM task failed: %s", e.getClass().getSimpleName());
            return GcmNetworkManager.RESULT_RESCHEDULE;
        }
    }
}
