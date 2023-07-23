package com.gaiagps.iburn;

import android.location.Location;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class Geo {

    public static final double MAN_LAT = 40.786400;
    public static final double MAN_LON = -119.203500;

    private static float[] sResult = new float[1];

    /**
     * @return Returns the distance between a start and end Location in meters
     */
    public static double getDistance(double startLat, double startLon, Location end) {
        Location.distanceBetween(startLat, startLon, end.getLatitude(), end.getLongitude(), sResult);
        return sResult[0];
    }

    /**
     * @return a walking estimate in minutes for a distance in meters
     */
    public static double getWalkingEstimateMinutes(double meters) {
        return 60 * (meters / 2500); // At 2.5 kph
    }

    /**
     * @return a bicycling estimate in minutes for a distance in meters
     */
    public static double getBikingEstimateMinutes(double meters) {
        return 60 * (meters / 7550); // At 7.75 kph
    }

}
