package com.gaiagps.iburn.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.gaiagps.iburn.AudioTourManager;

import static com.gaiagps.iburn.database.Art.TABLE_NAME;

/**
 * Created by dbro on 6/8/17.
 */

@Entity(tableName = TABLE_NAME)
public class Art extends PlayaItem {
    public static final String TABLE_NAME = "arts";

    public static final String ARTIST = "artist";
    public static final String ARTIST_LOCATION = "a_loc";
    public static final String IMAGE_URL = "i_url";
//    public static final String AUDIO_TOUR_URL = "a_url";


    @ColumnInfo(name = ARTIST)
    public String artist;

    @ColumnInfo(name = ARTIST_LOCATION)
    public String artistLocation;

    @ColumnInfo(name = IMAGE_URL)
    public String imageUrl;

//    @ColumnInfo(name = AUDIO_TOUR_URL)
//    public String audioTourUrl;

    public boolean hasAudioTour(@NonNull Context context) {
//        return !TextUtils.isEmpty(audioTourUrl);
        return AudioTourManager.hasAudioTour(context, playaId);
    }

    public boolean hasImage() {
        return !TextUtils.isEmpty(imageUrl);
    }

}