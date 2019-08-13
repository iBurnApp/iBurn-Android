package com.gaiagps.iburn.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;

import static com.gaiagps.iburn.database.UserPoi.TABLE_NAME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by dbro on 6/12/17.
 */

@Entity(tableName = TABLE_NAME)
public class UserPoi extends PlayaItem {
    public static final String TABLE_NAME = "user_pois";

    public static final String ICON = "icon";

    public static final String ICON_HEART = "heart";
    public static final String ICON_STAR = "star";
    public static final String ICON_BIKE = "bike";
    public static final String ICON_HOME = "home";

    @Retention(SOURCE)
    @StringDef({
            ICON_HEART,
            ICON_STAR,
            ICON_BIKE,
            ICON_HOME
    })
    public @interface Icon {}

    @ColumnInfo(name = ICON)
    @Icon
    public String icon;
}
