package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.log.DiskLogger;

import java.io.File;
import java.io.IOException;

import static com.gaiagps.iburn.adapters.Util.cancelLogSummaryAsync;
import static com.gaiagps.iburn.adapters.Util.setLogSummaryAsync;

/**
 * An Array adapter for displaying a collection of app log files in a Spinner.
 * Created by dbro on 12/15/16.
 */
public class AppLogSpinnerAdapter extends ArrayAdapter<File> {

    private File currentLogFile;

    public AppLogSpinnerAdapter(Context context) {
        super(context, R.layout.recyclerview_logfile_item);
    }

    /**
     * @param files          the list of log files to display
     * @param currentLogFile the current log file for special visual treatment. May be null.
     */
    public void setFiles(@NonNull File[] files, File currentLogFile) {
        this.currentLogFile = currentLogFile;
        clear();
        addAll(files);
    }

    // The view to present the selected item when list is hidden
    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spinner_logfile_selection, parent, false);
        }

        File file = getItem(position);
        if (filePathsEqual(currentLogFile, file)) {
            ((TextView) convertView.findViewById(R.id.title)).setText("This Session");
        } else {
            setLogSummaryAsync(file, (TextView) convertView.findViewById(R.id.title));
        }

        return convertView;
    }

    // The view that appears within the list of selection choices
    @Override
    public View getDropDownView(int position,
                                View convertView,
                                ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spinner_logfile_item, parent, false);
        }
        File file = getItem(position);
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        if (filePathsEqual(currentLogFile, file)) {
            title.setText(R.string.current_session);
            cancelLogSummaryAsync(title);
            subtitle.setText(R.string.in_progress);
        } else {
            title.setText(R.string.loading_summary);
            String subtitleStr = (String) DateUtils.getRelativeTimeSpanString(file.lastModified());
            if (file.getName().contains(DiskLogger.CRASHED_SESSION_FILENAME_SUFFIX) &&
                    convertView.getContext() != null) {
                subtitleStr += convertView.getContext().getString(R.string.app_crashed);
            }
            subtitle.setText(subtitleStr);
            setLogSummaryAsync(file, (TextView) convertView.findViewById(R.id.title));
        }
        return convertView;
    }

    private static boolean filePathsEqual(File f1, File f2) {
        try {
            if ((f1 == null || f2 == null)) {
                return false;
            } else {
                return f1.getCanonicalPath().equals(f2.getCanonicalPath());
            }
        } catch (IOException e) {
            return false;
        }
    }
}