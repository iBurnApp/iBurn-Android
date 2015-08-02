package com.gaiagps.iburn;

import android.content.Context;
import android.text.format.DateUtils;

import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by davidbrodsky on 8/6/14.
 */
public class DateUtil {

    private static SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.US);

    /**
     * Get a human description of an event's state
     * (e.g: Starts in XX, Ends in XX)
     *
     * @param context            The application {@link android.content.Context}
     * @param nowDate            The date to treat as 'now'
     * @param relativeTimeCutoff The date before which to use relative date descriptors. e.g: (in 2 minutes)
     * @param startDateStr       An ISO start date string
     * @param prettyStartDateStr A 'prettified' start date string
     * @param endDateStr         An ISO end date string
     * @param prettyEndDateStr   A 'prettified' end date string
     */
    public static String getDateString(Context context, Date nowDate, Date relativeTimeCutoff, String startDateStr, String prettyStartDateStr, String endDateStr, String prettyEndDateStr) {
        try {
            Date startDate = PlayaDateTypeAdapter.iso8601Format.parse(startDateStr);
            if (nowDate.before(startDate)) {
                // Has not yet started
                if (relativeTimeCutoff.after(startDate)) {
                    return context.getString(R.string.starts) + " " + DateUtils.getRelativeTimeSpanString(startDate.getTime(), nowDate.getTime(), DateUtils.MINUTE_IN_MILLIS).toString();
                }
                return context.getString(R.string.starts) + " " + prettyStartDateStr;
            } else {
                // Already started
                Date endDate = PlayaDateTypeAdapter.iso8601Format.parse(endDateStr);
                if (endDate.before(nowDate)) {
                    if (relativeTimeCutoff.after(endDate)) {
                        return context.getString(R.string.ended) + " " + DateUtils.getRelativeTimeSpanString(endDate.getTime(), nowDate.getTime(), DateUtils.MINUTE_IN_MILLIS).toString();
                    }
                    return context.getString(R.string.ended) + " " + prettyEndDateStr;
                } else {
                    if (relativeTimeCutoff.after(endDate)) {
                        return context.getString(R.string.ends) + " " + DateUtils.getRelativeTimeSpanString(endDate.getTime(), nowDate.getTime(), DateUtils.MINUTE_IN_MILLIS).toString();
                    }
                    return context.getString(R.string.ends) + " " + prettyEndDateStr;
                }

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return prettyStartDateStr;
    }

    public static String getStartDateString(Date startDate, Date nowDate) {

        if (Math.abs(startDate.getTime() - nowDate.getTime()) < DateUtils.MINUTE_IN_MILLIS) {
            return "Starting now";

        } else {
            long deltaTime = startDate.getTime() - nowDate.getTime();
            int hours = (int) (deltaTime / DateUtils.HOUR_IN_MILLIS);
            int minutes = (int) ((deltaTime - (hours * DateUtils.HOUR_IN_MILLIS)) / DateUtils.MINUTE_IN_MILLIS);

            String relativeSpan = null;
            if (hours > 0)
                relativeSpan = "at " + timeFormatter.format(startDate);
            else
                relativeSpan = String.format("in %d minute%s", minutes, minutes > 1 ? 's' : "");

            return (startDate.before(nowDate) ? "Started " : "Starts ") + relativeSpan;
        }
    }

    public static String getEndDateString(Date endDate, Date nowDate) {

        if (Math.abs(endDate.getTime() - nowDate.getTime()) < DateUtils.MINUTE_IN_MILLIS) {
            return "Ending now";

        } else {

            String relativeSpan = DateUtils.getRelativeTimeSpanString(
                    endDate.getTime(),
                    nowDate.getTime(),
                    DateUtils.MINUTE_IN_MILLIS).toString();
            return (endDate.before(nowDate) ? "Ended " : "Ends ") + relativeSpan;
        }
    }
}
