package com.gaiagps.iburn.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.PrimaryKey;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.Serializable;

/**
 * Created by dbro on 6/8/17.
 */

public class PlayaItem implements Serializable {
    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String DESC = "desc";
    public static final String URL = "url";
    public static final String CONTACT = "contact";
    public static final String PLAYA_ADDR = "p_addr";
    public static final String PLAYA_ADDR_UNOFFICIAL = "p_addr_unof";
    public static final String PLAYA_ID = "p_id";
    public static final String LATITUDE = "lat";
    public static final String LONGITUDE = "lon";
    public static final String LATITUDE_UNOFFICIAL = "lat_unof";
    public static final String LONGITUDE_UNOFFICIAL = "lon_unof";
    public static final String FAVORITE = "fav";


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    public int id;

    @ColumnInfo(name = NAME)
    public String name;

    @ColumnInfo(name = DESC)
    public String description;

    @ColumnInfo(name = URL)
    public String url;

    @ColumnInfo(name = CONTACT)
    public String contact;

    @ColumnInfo(name = PLAYA_ADDR)
    public String playaAddress;

    @ColumnInfo(name = PLAYA_ADDR_UNOFFICIAL)
    public String playaAddressUnofficial;

    @ColumnInfo(name = PLAYA_ID)
    public String playaId;

    @ColumnInfo(name = LATITUDE)
    public float latitude;

    @ColumnInfo(name = LONGITUDE)
    public float longitude;

    @ColumnInfo(name = LATITUDE_UNOFFICIAL)
    public float latitudeUnofficial;

    @ColumnInfo(name = LONGITUDE_UNOFFICIAL)
    public float longitudeUnofficial;

    @ColumnInfo(name = FAVORITE)
    public boolean isFavorite;

    public boolean hasLocation() {
        return latitude != 0 && longitude != 0;
    }

    public boolean hasUnofficialLocation() {
        return latitudeUnofficial != 0 && longitudeUnofficial != 0;
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    public LatLng getUnofficialLatLng() {
        return new LatLng(latitudeUnofficial, longitudeUnofficial);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayaItem playaItem = (PlayaItem) o;

        if (id != playaItem.id) return false;
        return playaId != null ? playaId.equals(playaItem.playaId) : playaItem.playaId == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (playaId != null ? playaId.hashCode() : 0);
        return result;
    }
}
