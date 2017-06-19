package com.gaiagps.iburn;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.gaiagps.iburn.database.Art;
import com.gaiagps.iburn.service.AudioPlayerService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Audio tour playback manager
 * Created by dbro on 8/5/16.
 */
public class AudioTourManager {

    private final OkHttpClient http = new OkHttpClient();

    private Context context;
    private Uri defaultAlbumArtUri;
    private File mediaArtDir;
    private File mediaDir;

    public static File getAudioTourDirectory(@NonNull Context context) {
        File audioTourDir = new File(context.getFilesDir(), "audio_tour");
        audioTourDir.mkdirs();
        return audioTourDir;
    }

    public static File getAudioTourArtDirectory(@NonNull Context context) {
        File audioTourArtDir = new File(context.getFilesDir(), "audio_tour_art");
        audioTourArtDir.mkdirs();
        return audioTourArtDir;
    }

    /**
     * Create a new AudioTourManager bound to the given Context.
     * Generally call this on {@link Activity#onResume()}
     *
     * @param context The Context to bind the player service to
     */
    public AudioTourManager(@NonNull Context context) {
        this.context = context;
//        player = PlayerHater.bind(context);
//        player.setLocalPlugin(new PlayerHaterListenerPlugin(listener));
        defaultAlbumArtUri = getResourceUri(context, R.drawable.iburn_logo);
        mediaArtDir = getAudioTourArtDirectory(context);
        mediaDir = getAudioTourDirectory(context);

        if (!mediaDir.exists() && !mediaArtDir.exists()) {
            Timber.e("Failed to create audio tour directories!");
        }
    }

    public void playAudioTourUrl(@NonNull Art art) {

        File cachedFile = getCachedFileForRemoteMediaPath(art.audioTourUrl);
        if (!cachedFile.exists()) {
            Toast.makeText(context, "Downloading '" + art.name + "' Tour. Playback will start when ready", Toast.LENGTH_LONG).show();
            cacheAndPlayRemoteMediaPath(art);
        } else {
            playLocalMediaForArt(Uri.fromFile(cachedFile), art);
        }
    }

    public Art getCurrentlyPlayingAudioTourArt() {
        return AudioPlayerService.Companion.getCurrentArt();
    }

    private void playLocalMediaForArt(@NonNull Uri localMediaUri, @NonNull Art art) {
        Art currentlyPlayingArt = getCurrentlyPlayingAudioTourArt();
        if (currentlyPlayingArt == null || !currentlyPlayingArt.equals(art)) {
            Uri albumArtUri = getFileImage(localMediaUri);
            AudioPlayerService.Companion.playAudioTour(context, localMediaUri, art, albumArtUri);
        } else {
            Timber.e("AudioPlayerService is already playing. Bind to service to issue control commands");
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

    static File getCachedFileForRemoteMediaPath(@NonNull Context context, @NonNull String mediaPath) {
        return new File(getAudioTourDirectory(context), mediaPath.substring(mediaPath.lastIndexOf("/"), mediaPath.length()));
    }

    private void cacheAndPlayRemoteMediaPath(@NonNull Art art) {
        Observable.just(art)
                .observeOn(Schedulers.io())
                .subscribe(ignored -> {

                    boolean didCache = cacheRemoteMediaPath(context, http, art.audioTourUrl);

                    if (didCache) {
                        File cachedFile = getCachedFileForRemoteMediaPath(art.audioTourUrl);
                        playLocalMediaForArt(Uri.fromFile(cachedFile), art);
                    }
                });
    }

    static boolean cacheRemoteMediaPath(@NonNull Context context, @NonNull OkHttpClient http, @NonNull String remoteMediaPath) {
        try {
            Request request = new Request.Builder()
                    .url(remoteMediaPath)
                    .build();

            long startTime = System.currentTimeMillis();
            Response response = http.newCall(request).execute();
            if (!response.isSuccessful()) {
                Timber.e("Media download of %s reported unexpected code %s", remoteMediaPath, response);
                return false;
            } else {
                Timber.d("Downloaded %s in %s ms", remoteMediaPath, System.currentTimeMillis() - startTime);
            }

            File destFile = getCachedFileForRemoteMediaPath(context, remoteMediaPath);
            FileOutputStream os = new FileOutputStream(destFile);
            InputStream is = response.body().byteStream();

            byte[] buff = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(buff)) > 0) {
                os.write(buff, 0, bytesRead);
            }
            is.close();
            os.close();

            return true;

        } catch (IOException e) {
            Timber.e(e, "Failed to fetch %s", remoteMediaPath);
        }
        return false;
    }
}
