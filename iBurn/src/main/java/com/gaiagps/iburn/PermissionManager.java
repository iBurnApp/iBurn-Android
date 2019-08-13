package com.gaiagps.iburn;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/**
 * Created by dbro on 3/3/16.
 */
public class PermissionManager {

    public static boolean hasLocationPermissions(@NonNull Context context) {
        return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
