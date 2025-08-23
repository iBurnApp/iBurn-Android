package com.gaiagps.iburn.api.response;

/**
 * Created by dbro on 8/24/16.
 */
public class Location {

    public String frontage;
    public String intersection;
    public String intersectionType;
    public double gps_latitude;
    public double gps_longitude;

    public static Location fromLocation(Location other) {
        Location result = new Location();
        result.frontage = other.frontage;
        result.intersection = other.intersection;
        result.intersectionType = other.intersectionType;
        result.gps_latitude = other.gps_latitude;
        result.gps_longitude = other.gps_longitude;
        return result;
    }

    public String locationString() {
        return String.format("%s %s %s", frontage, intersectionType, intersection).trim();
    }

    @Override
    public String toString() {
        return "Location{" +
                "string='" + locationString() + '\'' +
                ", gps_latitude=" + gps_latitude +
                ", gps_longitude=" + gps_longitude +
                '}';
    }
}
