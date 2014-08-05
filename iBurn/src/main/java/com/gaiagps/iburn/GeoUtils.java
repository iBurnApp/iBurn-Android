package com.gaiagps.iburn;

import android.location.Location;

/**
 * Created by davidbrodsky on 8/4/14.
 */
public class GeoUtils {

    /**
     * Returns the distance between a start and end Location
     * in m
     */
    public static double getDistance(double startLat, double startLon, Location end) {
        double theta = startLon - end.getLongitude();
        double dist = Math.sin(Math.toRadians(startLat)) * Math.sin(Math.toRadians(end.getLatitude())) +
                Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(end.getLatitude())) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1609.344 * 1000; // to meters
        return dist;
    }
}
