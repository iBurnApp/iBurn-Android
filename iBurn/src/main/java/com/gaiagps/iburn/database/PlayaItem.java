package com.gaiagps.iburn.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.PrimaryKey;

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
    public static final String PLAYA_ID = "p_id";
    public static final String LATITUDE = "lat";
    public static final String LONGITUDE = "lon";
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

    @ColumnInfo(name = PLAYA_ID)
    public String playaId;

    @ColumnInfo(name = LATITUDE)
    public float latitude;

    @ColumnInfo(name = LONGITUDE)
    public float longitude;

    @ColumnInfo(name = FAVORITE)
    public boolean isFavorite;
}
