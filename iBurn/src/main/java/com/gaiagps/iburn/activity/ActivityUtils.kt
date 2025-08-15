package com.gaiagps.iburn.activity

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class ActivityUtils {
}

fun setupEdgeToEdge(activity: androidx.appcompat.app.AppCompatActivity) {
    // Draw under status bar
    val window = activity.window
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val nightMode: Int =
            activity.getResources().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        controller.isAppearanceLightStatusBars = nightMode != Configuration.UI_MODE_NIGHT_YES
        window.setStatusBarColor(Color.TRANSPARENT)
        window.setNavigationBarColor(Color.TRANSPARENT)
    } else {
        window.getDecorView()
            .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }
    WindowCompat.setDecorFitsSystemWindows(window, false)
}