package com.gaiagps.iburn.log

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Created by dbro on 7/27/17.
 */
public fun emailLog(hostActivity: Activity, sessionLog: File) {
    // Add App Session log
    val context = hostActivity.applicationContext
    val sharedSessionLog = File(getSharedLogsDir(context), sessionLog.name)
    try {
        val files = ArrayList<File>()
        sessionLog.copyTo(sharedSessionLog)
        files.add(sharedSessionLog)
        val descriptionBuilder = StringBuilder("\n\n")

        val logSummary = analyzeAppLog(sessionLog).toString()
        descriptionBuilder.append("\nApp Log Summary:\n")
                .append(logSummary)
        emailLogs(hostActivity = hostActivity,
                title = "Gj Feedback",
                description = descriptionBuilder.toString(),
                logFiles = files)

    } catch (e: IOException) {
        Timber.e("Failed to copy session log to shared log dir")
    }
}

private fun emailLogs(hostActivity: Activity,
                      title: String,
                      description: String,
                      logFiles: List<File>) {

    val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
    emailIntent.type = "text/plain"
    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
            arrayOf("davidpbrodsky@gmail.com"))
    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "iBurn-Gj Bug Report")
    emailIntent.putExtra(Intent.EXTRA_TEXT, title + '\n' + description)

    val uris = java.util.ArrayList<Uri>()
    for (file in logFiles) {
        uris.add(Uri.fromFile(file))
    }
    emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    hostActivity.startActivity(Intent.createChooser(emailIntent, "Send Bug Report..."))
}

private fun getSharedLogsDir(context: Context): File {
    val sharedLogsDir = File(context.getExternalFilesDir(null), "iBurn-Gj-Logs")

    val createdDir = sharedLogsDir.exists() || sharedLogsDir.mkdirs()

    if (!createdDir) {
        Timber.e("Failed to create log directory %s", sharedLogsDir.absolutePath)
    }

    return sharedLogsDir
}