package com.gaiagps.iburn;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.io.File;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by dbro on 8/20/16.
 */
public class AudioTourDownloader {

    /**
     * Download all audio tours to the cache directory specified by {@link AudioTourManager}
     */
    public void downloadAudioTours(@NonNull Context context) {

        final OkHttpClient http = new OkHttpClient();

        DataProvider.getInstance(context)
                .flatMap(provider -> provider.observeTable(PlayaDatabase.ART, new String[]{ArtTable.audioTourUrl}, ArtTable.audioTourUrl + " IS NOT NULL"))
                .observeOn(Schedulers.io())
                .first()
                .map(SqlBrite.Query::run)
                .flatMap(cursor -> {

                    ArrayList<String> toDownloadUrls = new ArrayList<>();
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            String audioTourUrl = cursor.getString(cursor.getColumnIndex(ArtTable.audioTourUrl));
                            File audioTourCacheFile = AudioTourManager.getCachedFileForRemoteMediaPath(context, audioTourUrl);
                            if (!TextUtils.isEmpty(audioTourUrl) &&
                                    !audioTourCacheFile.exists()) {
                                toDownloadUrls.add(audioTourUrl);
                            }
                        } while (cursor.moveToNext());

                        return Observable.from(toDownloadUrls);
                    }
                    return Observable.empty();
                })
                .subscribe(toDownloadUrl -> {
                    boolean didCache = AudioTourManager.cacheRemoteMediaPath(context, http, toDownloadUrl);
                    Timber.d("Downloaded %s with success %b", toDownloadUrl, didCache);
                }, throwable -> Timber.e(throwable, "Failed to download audio tours"));
    }
}
