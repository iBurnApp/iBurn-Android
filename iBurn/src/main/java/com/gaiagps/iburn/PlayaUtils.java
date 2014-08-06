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
     * @param startDateStr
     * @param prettyStartDateStr
     * @param endDateStr
     * @param prettyEndDateStr
     * @return
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
