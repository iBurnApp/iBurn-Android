package com.gaiagps.iburn;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by davidbrodsky on 8/6/14.
 */
public class DateUtil {

    public static final TimeZone PLAYA_TIME_ZONE = TimeZone.getTimeZone("America/Los_Angeles");

    private static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("h:mm a", Locale.US);

    static {
        TIME_FORMATTER.setTimeZone(PLAYA_TIME_ZONE);
    }

    public static SimpleDateFormat getPlayaTimeFormat(String pattern) {
        SimpleDateFormat result = new SimpleDateFormat(pattern, Locale.US);
        result.setTimeZone(PLAYA_TIME_ZONE);
        return result;
    }

    // Handy for querying database by event start / end time
    public static SimpleDateFormat getIso8601Format() {
        return getPlayaTimeFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    public static SimpleDateFormat getIso8601DeepLinkFormat() {
        return getPlayaTimeFormat("yyyyMMdd'T'HH:mm:ssZ");
    }

    /**
     * Get a human description of an event's state
     * (e.g: Starts in XX, Ends in XX)
     *
     * @param context            The application {@link android.content.Context}
     * @param nowDate            The date to treat as 'now'
     * @param startDate          The start Date
     * @param prettyStartDateStr A 'prettified' start date string
     * @param endDate            The end Date
     * @param prettyEndDateStr   A 'prettified' end date string
     */
    public static String getDateString(Context context, Date nowDate, Date startDate, String prettyStartDateStr, Date endDate, String prettyEndDateStr) {
            return prettyStartDateStr + " - " + prettyEndDateStr;
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
                relativeSpan = "at " + TIME_FORMATTER.format(startDate);
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

    public static Date getAllDayStartDateTime(String day){
        /***
         * This gets the start time
         * we will assume is the datetime for events that are
         * actually all day events
         **/
        Date now = CurrentDateProvider.getCurrentDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.MONTH,Integer.valueOf(day.split("/")[0])-1);
        cal.set(Calendar.DATE,Integer.valueOf(day.split("/")[1]));
        cal.set(Calendar.HOUR_OF_DAY,10);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTime();
    }

    public static Date getAllDayEndDateTime(String day){
        /***
         * This gets the end time we will assume is the datetime
         * for events that are
         * actually all day events
         **/
        Date now = CurrentDateProvider.getCurrentDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.MONTH,Integer.valueOf(day.split("/")[0])-1);
        cal.set(Calendar.DATE,Integer.valueOf(day.split("/")[1]));
        cal.set(Calendar.HOUR_OF_DAY,20);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        Date newTime = cal.getTime();
        return newTime;
    }
}
