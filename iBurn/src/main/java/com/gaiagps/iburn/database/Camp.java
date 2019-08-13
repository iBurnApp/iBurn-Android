package com.gaiagps.iburn.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import static com.gaiagps.iburn.database.Camp.TABLE_NAME;

/**
 * Created by dbro on 6/8/17.
 */

@Entity(tableName = TABLE_NAME)
public class Camp extends PlayaItem {
    public static final String TABLE_NAME = "camps";

    public static final String HOMETOWN = "hometown";

    @ColumnInfo(name = HOMETOWN)
    public String hometown;
}