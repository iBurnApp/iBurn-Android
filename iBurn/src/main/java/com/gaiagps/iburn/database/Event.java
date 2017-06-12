package com.gaiagps.iburn.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.text.TextUtils;

import java.util.List;

import io.reactivex.Flowable;

import static com.gaiagps.iburn.database.Event.TABLE_NAME;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.NAME;

/**
 * Created by dbro on 6/8/17.
 */

@Entity(tableName = TABLE_NAME)
public class Event extends PlayaItem {
    public static final String TABLE_NAME = "events";

    public static final String TYPE = "artist";
    public static final String ALL_DAY = "all_day";
    public static final String CHECK_LOC = "check_loc";
    public static final String CAMP_PLAYA_ID = "c_id";
    public static final String START_TIME = "s_time";
    public static final String START_TIME_PRETTY = "s_time_p";
    public static final String END_TIME = "e_time";
    public static final String END_TIME_PRETTY = "e_time_p";



    @ColumnInfo(name = TYPE)
    public String type;

    @ColumnInfo(name = ALL_DAY)
    public boolean allDay;

    @ColumnInfo(name = CHECK_LOC)
    public boolean checkLocation;

    @ColumnInfo(name = CAMP_PLAYA_ID)
    public String campPlayaId;

    @ColumnInfo(name = START_TIME)
    public String startTime;

    @ColumnInfo(name = START_TIME_PRETTY)
    public String startTimePretty;

    @ColumnInfo(name = END_TIME)
    public String endTime;

    @ColumnInfo(name = END_TIME_PRETTY)
    public String endTimePretty;

    public boolean hasCampHost() {
        return !TextUtils.isEmpty(campPlayaId);
    }
}