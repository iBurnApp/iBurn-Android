package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SchedulersKt;
import com.gaiagps.iburn.log.AppLogReport;
import com.gaiagps.iburn.log.DiskLogger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import static com.gaiagps.iburn.log.LogAnalyzerKt.analyzeAppLog;

/**
 * Created by dbro on 2/14/17.
 */
public class Util {

    // Cache Log summaries in memory only so we get updated summaries as LogAnalyzer changes
    private static HashMap<File, String> summaryCache = new HashMap<>();

    /**
     * Cancel the request to set a log summary set via {@link #setLogSummaryAsync(File, TextView)}
     */
    public static void cancelLogSummaryAsync(TextView target) {
        target.setTag(R.id.view_tag_file, null);
    }

    public static void setLogSummaryAsync(File file, TextView target) {
        target.setTag(R.id.view_tag_file, file);
        Observable.just(file)
                .observeOn(SchedulersKt.getIoScheduler())
                .flatMap(file1 -> {

                    if (summaryCache.containsKey(file)) {
                        return Observable.just(summaryCache.get(file));
                    }

                    try {
                        AppLogReport report = analyzeAppLog(file1);
                        String logSummary = report.toString();

                        // Do not cache the current log file's summary, since it is ongoing
                        Context appContext = target.getContext().getApplicationContext();
                        File currentLogFile = DiskLogger.getSharedInstance(appContext).getCurrentLogFile();
                        if (!Objects.equals(currentLogFile, file)) {
                            summaryCache.put(file, logSummary);
                        }

                        return Observable.just(logSummary);
                    } catch (IOException e) {
                        Timber.e(e, "Failed to log file");
                        return Observable.empty();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(logSummary -> {
                    File taggedFile = (File) target.getTag(R.id.view_tag_file);
                    if (Objects.equals(taggedFile, file)) {
                        target.setText(logSummary);
                    }
                });
    }
}
