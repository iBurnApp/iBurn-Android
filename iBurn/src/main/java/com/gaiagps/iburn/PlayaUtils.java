package com.gaiagps.iburn;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by davidbrodsky on 8/6/14.
 */
public class PlayaUtils {

    /**
     * Get a human description of an event's state
     * (e.g: Starts in XX, Ends in XX)
     *
     * @param context The application {@link android.content.Context}
     * @param nowDate The date to treat as 'now'
     * @param relativeTimeCutoff The date before which to use relative date descriptors. e.g: (in 2 minutes)
     * @param startDateStr An ISO start date string
     * @param prettyStartDateStr A 'prettified' start date string
     * @param endDateStr An ISO end date string
     * @param prettyEndDateStr A 'prettified' end date string
     */
    public static String getDateString(Context context, Date nowDate, Date relativeTimeCutoff, String startDateStr, String prettyStartDateStr, String endDateStr, String prettyEndDateStr) {
        try {
            Date startDate = PlayaClient.parseISODate(startDateStr);
            if (nowDate.before(startDate)) {
                // Has not yet started
                if (relativeTimeCutoff.after(startDate)) {
                    return context.getString(R.string.starts) + " " + DateUtils.getRelativeTimeSpanString(startDate.getTime()).toString();
                }
                return context.getString(R.string.starts) + " " + prettyStartDateStr;
            } else {
                // Already started
                Date endDate = PlayaClient.parseISODate(endDateStr);
                if (endDate.before(nowDate)) {
                    if (relativeTimeCutoff.after(endDate)) {
                        return context.getString(R.string.ended) + " " + DateUtils.getRelativeTimeSpanString(endDate.getTime()).toString();
                    }
                    return context.getString(R.string.ended) + " " + prettyEndDateStr;
                } else {
                    if (relativeTimeCutoff.after(endDate)) {
                        return context.getString(R.string.ends) + " " + DateUtils.getRelativeTimeSpanString(endDate.getTime()).toString();
                    }
                    return context.getString(R.string.ends) + " " + prettyEndDateStr;
                }

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return prettyStartDateStr;
    }
}
