package com.gaiagps.iburn.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AppLogAdapter;
import com.gaiagps.iburn.adapters.AppLogSpinnerAdapter;
import com.gaiagps.iburn.log.AppLogUploaderKt;
import com.gaiagps.iburn.log.DiskLogger;

import java.io.File;
import java.util.Arrays;

/**
 * Presents available {@link DiskLogger} log sessions for feedback submission. If the current session is
 * chosen, this sends Video feedback. If a previous session is chosen, send a subset of log information.
 * Created by dbro on 12/15/16.
 */
public class FeedbackDialogFragment extends AppCompatDialogFragment implements AppLogAdapter.LogFileClickListener {

    private Spinner logSpinner;
    private EditText notesEditText;

    private File selectedLogFile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_feedback_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button sendButton = (Button) view.findViewById(R.id.button_send);
        notesEditText = (EditText) view.findViewById(R.id.notes);
        logSpinner = (Spinner) view.findViewById(R.id.spinner);

        sendButton.setOnClickListener(v -> {

            if (selectedLogFile != null) {
                AppLogUploaderKt.emailLog(getActivity(), selectedLogFile);
            }
            FeedbackDialogFragment.this.dismiss();
        });

        AppLogSpinnerAdapter appLogAdapter = new AppLogSpinnerAdapter(getContext());
        logSpinner.setAdapter(appLogAdapter);

        DiskLogger diskLogger = DiskLogger.getSharedInstance(getContext().getApplicationContext());
        File[] logFiles = diskLogger.getLogfiles();
        Arrays.sort(logFiles, 0, logFiles.length, (lhs, rhs) -> {
            long lhsDate = lhs.lastModified();
            long rhsDate = rhs.lastModified();

            if (rhs == diskLogger.getCurrentLogFile()) {
                rhsDate = Integer.MAX_VALUE;
            }

            if (lhs == diskLogger.getCurrentLogFile()) {
                lhsDate = Integer.MAX_VALUE;
            }

            return (int) (rhsDate - lhsDate); // Descending
        });

        appLogAdapter.setFiles(logFiles, diskLogger.getCurrentLogFile());
        selectedLogFile = logFiles[0];
    }

    @Override
    public void onLogFileSelected(@NonNull File file) {
        selectedLogFile = file;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public boolean isCurrentSessionLogSelected() {
        // logFiles are sorted in descending order, so the first item is always the current session log
        return logSpinner.getSelectedItemPosition() == 0;
    }
}
