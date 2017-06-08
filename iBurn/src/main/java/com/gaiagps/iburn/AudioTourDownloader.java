package com.gaiagps.iburn;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gaiagps.iburn.database.DataProvider;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
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
                .flatMap(DataProvider::observeArtWithAudioTour)
                .observeOn(Schedulers.io())
                .flatMap(Observable::fromIterable)
                .subscribe(art -> {
                    String downloadUrl = art.getAudioTourUrl();
                    String audioTourUrl = art.getAudioTourUrl();
                    File audioTourCacheFile = AudioTourManager.getCachedFileForRemoteMediaPath(context, audioTourUrl);
                    if (!TextUtils.isEmpty(audioTourUrl) &&
                            !audioTourCacheFile.exists()) {
                        boolean didCache = AudioTourManager.cacheRemoteMediaPath(context, http, downloadUrl);
                        Timber.d("Downloaded %s with success %b", audioTourUrl, didCache);
                    }
                }, throwable -> Timber.e(throwable, "Failed to download audio tours"));
    }
    // TODO : Resume on error
}
