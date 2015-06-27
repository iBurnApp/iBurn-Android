package com.gaiagps.iburn;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.gaiagps.iburn.api.IBurnService;

import java.util.ArrayList;

public class DataUtils {
    private static final String TAG = "DataUtils";

    //Location of THE MAN
    private static final double MAN_LAT = 40.782818;
    private static final double MAN_LON = -119.209042;

    public static final double MAN_DISTANCE_THRESHOLD = 3; // miles

    /**
     * @param c
     * @return true if database is ready, false if setup required
     */
    public static boolean checkAndSetupDB(Context c) {

        if (!PlayaClient.isDbPopulated(c)) {
            Log.i(TAG, "Populating db");
            IBurnService service = new IBurnService(c);
            service.updateData();
            // TODO : Report success
            return false;
        } else {
            Log.i(TAG, "Database already populated with json");
            return true;
        }
    }


    public static double distanceFromTheMan(double lat, double lon) {
        //40.782818, -119.209042

        double theta = lon - MAN_LON;
        double dist = Math.sin(deg2rad(lat)) * Math.sin(deg2rad(MAN_LAT)) + Math.cos(deg2rad(lat)) * Math.cos(deg2rad(MAN_LAT)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return dist; // miles
    }

    static private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    static private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    public static int bulkInsertContentValues(Context c, ArrayList<ContentValues> cv, Uri uri){
        if(c == null)
            return 0;
        int result;
        ContentValues[] cvList = new ContentValues[1];
        cvList = cv.toArray(cvList); // toArray requires an initialized array of type equal to desired result :/
        result = c.getContentResolver().bulkInsert(uri, cvList);
        c.getContentResolver().notifyChange(uri, null);
        return result;
    }


}
