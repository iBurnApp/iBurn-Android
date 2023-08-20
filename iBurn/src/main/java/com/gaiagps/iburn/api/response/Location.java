package com.gaiagps.iburn.api.response;

/**
 * Created by dbro on 8/24/16.
 */
public class Location {

    public String string;
    public double gps_latitude;
    public double gps_longitude;

    @Override
    public String toString() {
        return "Location{" +
                "string='" + string + '\'' +
                ", gps_latitude=" + gps_latitude +
                ", gps_longitude=" + gps_longitude +
                '}';
    }
}
