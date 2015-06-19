package com.gaiagps.iburn;

import android.location.Location;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class GeoUtils {

    private static float[] sResult = new float[1];

    /**
     * Returns the distance between a start and end Location
     * in meters
     */
    public static double getDistance(double startLat, double startLon, Location end) {
        Location.distanceBetween(startLat, startLon, end.getLatitude(), end.getLongitude(), sResult);
        return sResult[0];
    }
}
