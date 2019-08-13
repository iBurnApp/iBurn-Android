package com.gaiagps.iburn;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by dbro on 8/20/16.
 */
public class AudioTourDownloader {

    /**
     * Download all audio tours to the cache directory specified by {@link AudioTourManager}
     */
//    public void downloadAudioTours(@NonNull Context context) {
//
//        final OkHttpClient http = new OkHttpClient();
//
//        DataProvider.Companion.getInstance(context)
//                .flatMap(provider -> provider.observeArtWithAudioTour().toObservable())  // TODO : Rm toObservable
//                .observeOn(Schedulers.io())
//                .flatMap(Observable::fromIterable)
//                .subscribe(art -> {
//                    String audioTourUrl = art.audioTourUrl;
//                    File audioTourCacheFile = AudioTourManager.getCachedFileForRemoteMediaPath(context, audioTourUrl);
//                    if (!TextUtils.isEmpty(audioTourUrl) &&
//                            !audioTourCacheFile.exists()) {
//                        boolean didCache = AudioTourManager.cacheRemoteMediaPath(context, http, audioTourUrl);
//                        Timber.d("Downloaded %s with success %b", audioTourUrl, didCache);
//                    }
//                }, throwable -> Timber.e(throwable, "Failed to download audio tours"));
//    }
    // TODO : Resume on error
}
