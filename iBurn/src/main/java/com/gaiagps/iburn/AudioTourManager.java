package com.gaiagps.iburn;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.prx.playerhater.PlayerHater;
import org.prx.playerhater.PlayerHaterListener;
import org.prx.playerhater.Song;
import org.prx.playerhater.plugins.PlayerHaterListenerPlugin;

/**
 * Audio tour playback manager
 * Created by dbro on 8/5/16.
 */
public class AudioTourManager {

    private PlayerHater player;
    private Uri albumArtUri;

    /**
     * Create a new AudioTourManager bound to the given Context.
     * Generally call this on {@link Activity#onResume()}
     *
     * @param context The Context to bind the player service to
     */
    public AudioTourManager(@NonNull Context context, @Nullable PlayerHaterListener listener) {
        this.player = PlayerHater.bind(context);
        this.player.setLocalPlugin(new PlayerHaterListenerPlugin(listener));
        albumArtUri = getResourceUri(context, R.drawable.iburn_logo);
    }

    /**
     * Unbind the player
     */
    public void release() {
        player.release();
    }


    public void playAudioTourUrl(@NonNull String audioTourUrl, @Nullable String artTitle) {
        String curTourUrl = getCurrentAudioTourUrl();
        if (curTourUrl != null && curTourUrl.equals(audioTourUrl)) {
            player.play();
        } else {
            player.play(songFromAudioTourUrl(audioTourUrl, artTitle));
        }
    }

    public String getCurrentAudioTourUrl() {
        return player.nowPlaying() != null ? player.nowPlaying().getUri().toString() : null;
    }

    public void pause() {
        player.pause();
    }

    public Song songFromAudioTourUrl(@NonNull String audioTourUrl,
                                     @Nullable String artName) {
        return new Song() {
            @Override
            public String getTitle() {
                return artName;
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
                return albumArtUri;
            }

            @Override
            public Uri getUri() {
                return Uri.parse(audioTourUrl);
            }

            @Override
            public Bundle getExtra() {
                return null;
            }
        };
    }

    private static Uri getResourceUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                resID);
    }
}
