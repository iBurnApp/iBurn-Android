/*
 * AppLogAdapter.java
 *
 * The contents of this file are confidential and proprietary to Pearl Automation Inc.
 *
 * Any use, reproduction, distribution, and/or transfer of this file is strictly
 * prohibited without the express written permission of the current copyright
 * owner.
 *
 * Any licensed derivative work must retain this notice.
 *
 * Copyright (c) 2016, Pearl Automation Inc. All Rights Reserved.
 */

package com.gaiagps.iburn.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.log.DiskLogger;

import java.io.File;

import static com.gaiagps.iburn.adapters.Util.setLogSummaryAsync;


/**
 * An adapter for displaying a collection of app log files.
 * Created by dbro on 1/30/16.
 */
public class AppLogAdapter extends RecyclerView.Adapter<AppLogAdapter.ViewHolder> {
    private File[] logFiles;
    private LogFileClickListener itemClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        protected TextView title;
        protected TextView subtitle;

        public ViewHolder(View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.title);
            this.subtitle = (TextView) view.findViewById(R.id.subtitle);
        }
    }

    public interface LogFileClickListener {
        void onLogFileSelected(@NonNull File file);
    }

    public AppLogAdapter(LogFileClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setFiles(File[] files) {
        this.logFiles = files;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recyclerview_logfile_item, viewGroup, false);

        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.itemView.setOnClickListener(v -> {
            File associatedLogFile = (File) v.getTag();
            if (associatedLogFile != null && itemClickListener != null) {
                itemClickListener.onLogFileSelected(associatedLogFile);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        File file = logFiles[i];

        viewHolder.itemView.setTag(file);
        setLogSummaryAsync(file, viewHolder.title);

        String subtitle = (String) DateUtils.getRelativeTimeSpanString(file.lastModified());
        if (file.getName().contains(DiskLogger.CRASHED_SESSION_FILENAME_SUFFIX)) {
            subtitle += " - App crashed";
        }
        viewHolder.subtitle.setText(subtitle);
    }

    @Override
    public int getItemCount() {
        return (null != logFiles ? logFiles.length : 0);
    }

}