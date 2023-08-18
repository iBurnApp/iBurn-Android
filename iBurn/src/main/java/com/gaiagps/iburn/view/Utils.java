package com.gaiagps.iburn.view;

import com.gaiagps.iburn.DateUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
        TimeZone tz = DateUtil.PLAYA_TIME_ZONE;
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        formatter.setTimeZone(tz);
        String datetime_string = formatter.format(datetime);
        return datetime_string;
    }


}
