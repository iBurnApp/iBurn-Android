package com.gaiagps.iburn.gj.message;

import com.google.android.gms.maps.model.LatLng;

public class GjMessageReportGps extends GjMessageString {

    public GjMessageReportGps(LatLng latLng) {
        super(Type.ReportGps, latLng.toString());
    }
}
