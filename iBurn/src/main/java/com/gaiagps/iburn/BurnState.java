package com.gaiagps.iburn;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.gms.maps.model.LatLng;

import java.util.GregorianCalendar;

/**
 * This class keeps track of application-wide state
 * Created by davidbrodsky on 8/4/13.
 */
public class BurnState {
    public static final String UNLOCK_PW = "burnbabyburn";

    public static boolean isEmbargoClear(Context c){
        boolean isClear =  c.getSharedPreferences(Constants.GENERAL_PREFS, Context.MODE_PRIVATE).getBoolean(Constants.EMBARGO_CLEAR, false);
        if(!isClear && Constants.EMBARGO_DATE.before(new GregorianCalendar())){
            setEmbargoClear(c, true);
            isClear = !isClear;
        }
        return isClear;
    }

    public static void setEmbargoClear(Context c, boolean isClear){
        c.getSharedPreferences(Constants.GENERAL_PREFS, Context.MODE_PRIVATE).edit().putBoolean(Constants.EMBARGO_CLEAR, isClear).commit();
        return;
    }

    public static LatLng getHomeLatLng(Context c){
        SharedPreferences prefs = c.getSharedPreferences(Constants.GENERAL_PREFS, Context.MODE_PRIVATE);
        double lat = prefs.getFloat(Constants.HOME_LAT, 0);
        double lon = prefs.getFloat(Constants.HOME_LON, 0);
        if(lat == 0 || lon == 0)
            return null;
        else
            return new LatLng(lat, lon);
    }

    public static void setHomeLatLng(Context c, LatLng latLng){
        SharedPreferences.Editor editor = c.getSharedPreferences(Constants.GENERAL_PREFS, Context.MODE_PRIVATE).edit();
        editor.putFloat(Constants.HOME_LAT, (float) latLng.latitude);
        editor.putFloat(Constants.HOME_LON, (float) latLng.longitude);
        editor.commit();
    }
}
