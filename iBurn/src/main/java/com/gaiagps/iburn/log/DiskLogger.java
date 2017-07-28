package com.gaiagps.iburn.log;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gaiagps.iburn.BuildConfig;
import com.gaiagps.iburn.DeviceUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import timber.log.Timber;

/**
 * A mechanism to persist app logs to disk.
 * Created by dbro on 2/18/16.
 */
public class DiskLogger extends Timber.DebugTree {

    /**
     * Maximum age of logs to keep during calls to
     */
    private static final long MAX_LOG_AGE_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    private static final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    static {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Remember to not use Timber in here to avoid recursive doom! Timber logs are dispatched here!
    private static final String TAG = "DiskLogger";

    /**
     * Log directory name within internal storage returned by {@link Context#getFilesDir()}
     */
    private static final String LOCAL_APP_LOG_DIRECTORY = "app_logs";

    private static final String LOG_FILE_EXT = "log";

    /**
     * Log filename suffix for session files that stopped due to an uncaught exception
     */
    public static final String CRASHED_SESSION_FILENAME_SUFFIX = "-crashed";

    private File logDir;
    private File logFile;
    private Date sessionStart;
    private BufferedWriter logFileOutputStream;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ssa", Locale.US);
    private boolean hasLogContents = false;  // Whether this session has written at least one log item

    private static DiskLogger diskLogger;

    public static DiskLogger getSharedInstance(@NonNull Context context) {
        if (diskLogger == null) {
            diskLogger = new DiskLogger(context);
        }
        return diskLogger;
    }

    private DiskLogger(@NonNull Context context) {
        this.logDir = new File(context.getFilesDir(), LOCAL_APP_LOG_DIRECTORY);
        logDir.mkdirs();
    }

    /**
     * @return a list of existing log files. Be sure to call this after
     * {@link #stopSession()} to avoid collecting partially complete logs
     */
    public File[] getLogfiles() {
        return logDir.listFiles();
    }

    public File getCurrentLogFile() {
        return logFile;
    }

    public synchronized void startSesionIfNotStarted() {
        if (logFileOutputStream != null) return;
        startSession();
    }

    public synchronized void startSession() {
        Log.d(TAG, "Starting log session");
        stopSession();
        hasLogContents = false;
        sessionStart = new Date();
        String logfileName = dateFormatter.format(sessionStart) + "." + LOG_FILE_EXT;
        logFile = new File(logDir, logfileName);
        try {
            boolean created = logFile.createNewFile();
            if (!created) throw new IOException();

            logFileOutputStream = new BufferedWriter(new FileWriter(logFile));
            String versionString = String.format(
                    Locale.US,
                    "%s version %s build %d variant %s-%s device %s android %d\n",
                    BuildConfig.APPLICATION_ID,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.FLAVOR,
                    BuildConfig.BUILD_TYPE,
                    DeviceUtil.getDeviceName(),
                    Build.VERSION.SDK_INT);
            logFileOutputStream.write(versionString);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open log file", e);
        }
    }

    /**
     * Delete logs older than {@link #MAX_LOG_AGE_MS}
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        File[] files = getLogfiles();
        final int totalLogCount = files.length;
        AtomicInteger oldLogCount = new AtomicInteger(0);

        Observable.fromArray(files)
                .filter(file -> now - file.lastModified() > MAX_LOG_AGE_MS)
                .doOnNext(ignored -> oldLogCount.incrementAndGet())
                .flatMap(file -> Observable.just(file.delete()))
                .reduce(0, (successCount, thisSuccess) -> thisSuccess ? successCount + 1 : successCount)
                .subscribe(successCount -> {
                    Timber.d("Deleted %d / %d log files due to old age. %d deletes failed", successCount, totalLogCount, oldLogCount.get() - successCount);
                }, throwable -> Timber.e(throwable, "Failed to cleanup old logs"));
    }

    @Nullable
    public synchronized File stopSession() {
        return stopSession(false);
    }

    private File stopSession(boolean withException) {
        sessionStart = null;
        if (logFileOutputStream != null) {
            try {
                logFileOutputStream.flush();
                logFileOutputStream.close();
                logFileOutputStream = null;
                Log.d(TAG, "Stopped session. " + logFile.length() + " byte log with contents: " + hasLogContents);
                if (!hasLogContents) {
                    logFile.delete();
                    return null;
                }
                if (withException) {
                    String dotPlusExt = "." + LOG_FILE_EXT;
                    String crashedPath = logFile.getAbsolutePath().replace(dotPlusExt, CRASHED_SESSION_FILENAME_SUFFIX + dotPlusExt);
                    logFile.renameTo(new File(crashedPath));
                }
                return logFile;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close log file", e);
            }
        }
        // If this method is called repeatedly after startSession(), return the last log
        return logFile;
    }

    protected synchronized File stopSessionWithUncaughtException(Throwable t) {
        writeLog(Log.ERROR, "Crash", "Process terminating after uncaught Exception", t);
        return stopSession(true);
    }

    @Override
    protected synchronized void log(int priority, String tag, String message, Throwable t) {
        if (BuildConfig.DEBUG) super.log(priority, tag, message, t);
        writeLog(priority, tag, message, t);
    }

    private void writeLog(int priority, String tag, String message, Throwable t) {
        if (logFileOutputStream != null) {
            try {
                cal.setTimeInMillis(System.currentTimeMillis());
                logFileOutputStream.write(sdf.format(cal.getTime()));
                logFileOutputStream.write(" ");
                logFileOutputStream.write(describePriority(priority));
                logFileOutputStream.write(" ");
                logFileOutputStream.write(tag == null ? "" : tag);
                logFileOutputStream.write(" : ");
                logFileOutputStream.write(message);
                logFileOutputStream.write("\n");
                if (t != null) {
                    logFileOutputStream.write(Log.getStackTraceString(t));
                }
                hasLogContents = true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to write to log file");
            }
        } else {
            Log.w(TAG, "No FileOutputStream available to write log: " + message);
        }
    }

    private static String describePriority(int priority) {

        switch (priority) {
            case Log.VERBOSE:
                return "VERBOSE";
            case Log.DEBUG:
                return "DEBUG";
            case Log.INFO:
                return "INFO";
            case Log.WARN:
                return "WARN";
            case Log.ERROR:
                return "ERROR";
            case Log.ASSERT:
                return "ASSERT";
            default:
                return "UNKNOWN";
        }
    }
}