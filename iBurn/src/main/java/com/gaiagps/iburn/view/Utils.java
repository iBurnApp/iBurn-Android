package com.gaiagps.iburn.view;

import com.gaiagps.iburn.CurrentDateProvider;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Calendar;

/**
 * From https://github.com/jd-alexander/LikeButton
 * Created by Joel on 23/12/2015.
 */
public class Utils {
    public static double mapValueFromRangeToRange(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
        return toLow + ((value - fromLow) / (fromHigh - fromLow) * (toHigh - toLow));
    }

    public static double clamp(double value, double low, double high) {
        return Math.min(Math.max(value, low), high);
    }

    public static String convertDateToString(Date datetime){
        TimeZone tz = TimeZone.getTimeZone("GMT-07:00");
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        formatter.setTimeZone(tz);
        String datetime_string = formatter.format(datetime);
        return datetime_string;
    }


}
