package com.gaiagps.iburn;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import org.prx.playerhater.PlayerHater;
import org.prx.playerhater.PlayerHaterListener;
import org.prx.playerhater.Song;
import org.prx.playerhater.plugins.PlayerHaterListenerPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Audio tour playback manager
 * Created by dbro on 8/5/16.
 */
public class AudioTourManager {

    private final OkHttpClient http = new OkHttpClient();

    private Context context;
    private PlayerHater player;
    private Uri defaultAlbumArtUri;
    private File mediaArtDir;
    private File mediaDir;

    /**
     * Create a new AudioTourManager bound to the given Context.
     * Generally call this on {@link Activity#onResume()}
     *
     * @param context The Context to bind the player service to
     */
    public AudioTourManager(@NonNull Context context, @Nullable PlayerHaterListener listener) {
        this.context = context;
        player = PlayerHater.bind(context);
        player.setLocalPlugin(new PlayerHaterListenerPlugin(listener));
        defaultAlbumArtUri = getResourceUri(context, R.drawable.iburn_logo);
        mediaArtDir = new File(context.getFilesDir(), "audio_tour_art");
        mediaDir = new File(context.getFilesDir(), "audio_tour");

        boolean madeDirs;
        madeDirs = mediaArtDir.mkdirs() & mediaDir.mkdirs();
        madeDirs &= mediaDir.mkdirs();

        if (!madeDirs & (!mediaDir.exists() && !mediaArtDir.exists())) {
            Timber.e("Failed to create audio tour directories!");
        }
    }

    /**
     * Unbind the player
     */
    public void release() {
        player.release();
    }


    public void playAudioTourUrl(@NonNull String audioTourUrl, @Nullable String title) {

        File cachedFile = getCachedFileForRemoteMediaPath(audioTourUrl);
        if (!cachedFile.exists()) {
            Toast.makeText(context, "Downloading '" + title + "' Tour. Playback will start when ready", Toast.LENGTH_LONG).show();
            cacheAndPlayRemoteMediaPath(audioTourUrl, title);
        } else {
            playLocalMediaUrl(Uri.fromFile(cachedFile), title);
        }
    }

    public Uri getCurrentAudioTourUrl() {
        return player.nowPlaying() != null ? player.nowPlaying().getUri() : null;
    }

    public void pause() {
        player.pause();
    }

    public Song songFromAudioTourUrl(@NonNull Uri audioTourUri,
                                     @Nullable String title) {
        return new Song() {
            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getArtist() {
                return "Jim Tierney";
            }

            @Override
            public String getAlbumTitle() {
                return "Art Discovery Audio Guide";
            }

            @Override
            public Uri getAlbumArt() {
                Uri artUri = getFileImage(audioTourUri);
                context.grantUriPermission("com.android.systemui", artUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Timber.d("Returning album art uri %s", artUri);
                return artUri;
            }

            @Override
            public Uri getUri() {
                return audioTourUri;
            }

            @Override
            public Bundle getExtra() {
                return null;
            }
        };
    }

    private void playLocalMediaUrl(@NonNull Uri localMediaUri, @Nullable String title) {
        Uri curTourUrl = getCurrentAudioTourUrl();
        if (curTourUrl != null && curTourUrl.equals(localMediaUri)) {
            player.play();
        } else {
            player.play(songFromAudioTourUrl(localMediaUri, title));
        }
    }

    private static Uri getResourceUri(@NonNull Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                resID);
    }

    private Uri getFileImage(@NonNull Uri remoteMediaPath) {

        File artFile = getMediaArtFileForMediaPath(remoteMediaPath);
        if (artFile.exists()) {
            return getContentUriForFile(artFile);
        } else {
            Timber.d("Parsing image for audio %s into %s", remoteMediaPath, artFile.getAbsolutePath());

            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(context, remoteMediaPath);

            byte[] data = metadataRetriever.getEmbeddedPicture();
            if (data != null) {
                Timber.d("Got %d byte embedded picture for %s", data.length, remoteMediaPath);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                try {
                    FileOutputStream fos = new FileOutputStream(artFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                    bitmap.recycle();
                    return getContentUriForFile(artFile);
                } catch (FileNotFoundException e) {
                    Timber.e(e, "Failed to save album art");
                }
            }
            return defaultAlbumArtUri;
        }
    }

    private Uri getContentUriForFile(@NonNull File file) {
        return FileProvider.getUriForFile(
                context,
                "com.gaiagps.iburn.fileprovider",
                file);
    }

    private File getMediaArtFileForMediaPath(@NonNull Uri mediaPath) {
        String strPath = mediaPath.toString();
        return new File(mediaArtDir, strPath.substring(strPath.lastIndexOf("/"), strPath.lastIndexOf(".")) + ".jpg");
    }

    private File getCachedFileForRemoteMediaPath(@NonNull String mediaPath) {
        return new File(mediaDir, mediaPath.substring(mediaPath.lastIndexOf("/"), mediaPath.length()));
    }

    private void cacheAndPlayRemoteMediaPath(@NonNull String remoteMediaPath, @Nullable String title) {
        Observable.just(remoteMediaPath)
                .observeOn(Schedulers.io())
                .subscribe(path -> {

                    try {
                        Request request = new Request.Builder()
                                .url(remoteMediaPath)
                                .build();

                        long startTime = System.currentTimeMillis();
                        Response response = http.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            Timber.e("Media download of %s reported unexpected code %s", remoteMediaPath, response);
                            return;
                        } else {
                            Timber.d("Downloaded %s in %s ms", remoteMediaPath, System.currentTimeMillis() - startTime);
                        }

                        File destFile = getCachedFileForRemoteMediaPath(remoteMediaPath);
                        FileOutputStream os = new FileOutputStream(destFile);
                        InputStream is = response.body().byteStream();

                        byte[] buff = new byte[1024];
                        int bytesRead = 0;
                        while ((bytesRead = is.read(buff)) > 0) {
                            os.write(buff, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        playLocalMediaUrl(Uri.fromFile(destFile), title);

                    } catch (IOException e) {
                        Timber.e(e, "Failed to fetch %s", remoteMediaPath);
                    }
                });
    }
}
