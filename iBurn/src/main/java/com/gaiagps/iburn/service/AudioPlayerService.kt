package com.gaiagps.iburn.service

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.text.TextUtils
import com.gaiagps.iburn.R
import com.gaiagps.iburn.database.Art
import timber.log.Timber
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.res.AssetFileDescriptor
import android.support.v4.media.MediaMetadataCompat
import com.gaiagps.iburn.getAssetPathFromAssetUri
import com.gaiagps.iburn.isAssetUri


const val ExtraArtItem = "ArtItem"
const val ExtraLocalMediaUri = "LocalMedia"
const val ExtraAlbumArtUri = "AlbumArt"

const val NotificationId = 420420

const val NotificationChannelId = "420420"
const val NotificationChannelName = "AudioTour"
const val NotificationChannelDescription = "Playback controls for Audio Tour audio"

const val MediaMetadataKeyArtPlayaId = "ArtPlayaId"

class AudioPlayerService : MediaBrowserServiceCompat(), MediaPlayer.OnPreparedListener {

    companion object {
        var isPlaying = false
            private set

        var currentArt: Art? = null
            private set

        fun playAudioTour(context: Context,
                          localMediaUrl: Uri,
                          art: Art,
                          albumArtUri: Uri) {
            val intent = Intent(context, AudioPlayerService::class.java)
            intent.putExtra(ExtraArtItem, art)
            intent.putExtra(ExtraAlbumArtUri, albumArtUri.toString())
            intent.putExtra(ExtraLocalMediaUri, localMediaUrl.toString())
            context.startService(intent)
        }
    }

    private val LogTag = "iBurnAudioPlayer"

    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer()
    }

    private val mediaSession: MediaSessionCompat by lazy {
        MediaSessionCompat(applicationContext, LogTag)
    }

    private var currentAlbumArtUri: Uri? = null

//    private var stateBuilder: PlaybackStateCompat.Builder? = null

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        //  Browsing not yet supported
        if (TextUtils.isEmpty(parentMediaId)) {
            result.sendResult(null)
            return
        }
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        // Do not provide media browsing capability
        return MediaBrowserServiceCompat.BrowserRoot("", null)
    }

    override fun onCreate() {
        super.onCreate()

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
        mediaSession.setPlaybackState(stateBuilder.build())

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            // These callbacks are fired in response to user interaction with the media notification
            override fun onPlay() {
                Timber.d("onPlay")
                resumePlayback()
            }

            override fun onPause() {
                super.onPause()
                Timber.d("onPause")
                pausePlayback()
            }

            override fun onStop() {
                Timber.d("onStop")
                super.onStop()
                stopSelf()
            }
        })

        sessionToken = mediaSession.sessionToken
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.extras?.getSerializable(ExtraArtItem)?.let { art ->

            // Always stop playback to ensure MediaPlayer is in correct state
            stopPlayback()

            val extras = intent.extras
            val art = art as Art
            val albumArtUri = Uri.parse(extras.getString(ExtraAlbumArtUri))
            val mediaUri = Uri.parse(extras.getString(ExtraLocalMediaUri))

            currentAlbumArtUri = albumArtUri
            currentArt = art

            Timber.d("Attempting to play audio $mediaUri for ${art.name}")

            // Attach Art to this Media Session's Metadata. This allows clients
            // to determine which art is being played
            val metaBuilder = MediaMetadataCompat.Builder()
            metaBuilder.putString(MediaMetadataKeyArtPlayaId, art.playaId)
            mediaSession.setMetadata(metaBuilder.build())

            val notification = createNotificationBuilder(
                    art = art,
                    albumArtUri = albumArtUri,
                    isPlaying = true)

            mediaPlayer.setOnPreparedListener(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Timber.d("Using Android O AudioAttributes")
                // Use AudioAttributes
                val audioAttribs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                mediaPlayer.setAudioAttributes(audioAttribs.build())

            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }

            if (isAssetUri(mediaUri)) {
                val assetPath = getAssetPathFromAssetUri(mediaUri)
                val assetDescriptor = assets.openFd(assetPath)
                Timber.d("Playing audio from asset $assetPath")
                mediaPlayer.setDataSource(assetDescriptor.fileDescriptor, assetDescriptor.startOffset, assetDescriptor.length)
                assetDescriptor.close()
            } else {
                mediaPlayer.setDataSource(applicationContext, mediaUri)
            }
            // TODO : Catch and attempt recover from IllegalStateException
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnCompletionListener {
                Timber.d("Playback complete. Stopping foreground service2")
                stopPlayback()
                stopForeground(true)
                cancelNotification()
                stopSelf()
                mediaSession.setMetadata(null)
            }
            // MediaPlayer readiness reported to [onPrepared]

            startForeground(NotificationId, notification.build())
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationBuilder(art: Art, albumArtUri: Uri?, isPlaying: Boolean): android.support.v4.app.NotificationCompat.Builder {

        val notification = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.zzz_ph_ic_notification)
                .setContentTitle(art.name)
                .setContentText(art.artist)
                .setSound(null)
                .setSubText("Art Discovery Audio Guide by Jim Tierney")
                .setStyle(MediaStyle()
                        .setMediaSession(mediaSession.sessionToken))

                // Stop the service when the notification is swiped away
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))

                // Make the transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Add a pause button
                .addAction(
                        if (isPlaying)
                            android.support.v4.app.NotificationCompat.Action(
                                    R.drawable.ic_pause_black_24dp, "Pause",
                                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                            PlaybackStateCompat.ACTION_PLAY_PAUSE))
                        else
                            android.support.v4.app.NotificationCompat.Action(
                                    R.drawable.ic_play_arrow_black_24dp, "Play",
                                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                            PlaybackStateCompat.ACTION_PLAY_PAUSE)))


        if (albumArtUri != null) {

            val imageStream =
                    if (isAssetUri(albumArtUri)) {
                        applicationContext.assets.open(getAssetPathFromAssetUri(albumArtUri))
                    } else {
                        contentResolver.openInputStream(albumArtUri)
                    }

            val bitmap = BitmapFactory.decodeStream(imageStream)
            notification.setLargeIcon(bitmap)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Using Android O Notification channels")

            // Use Notification Channels
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                    NotificationChannelId,
                    NotificationChannelName,
                    importance)
            channel.description = NotificationChannelDescription
            notification.setChannelId(NotificationChannelId)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return notification
    }

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        Timber.d("Player prepared. Playing")
        mediaPlayer?.start()
        updatePlaybackState(true)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        mediaPlayer.release()
    }

    private fun resumePlayback() {
        Timber.d("ResumePlayback")
        mediaPlayer.start()
        updatePlaybackState(true)
        updateNotification()
    }

    private fun pausePlayback() {
        Timber.d("PausePlayback")
        mediaPlayer.pause()
        updatePlaybackState(false)
        updateNotification()
    }

    private fun stopPlayback() {
        Timber.d("StopPlayback")
        currentArt = null
        mediaPlayer.stop()
        mediaPlayer.reset()
        updatePlaybackState(false)
        updateNotification()
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE)

        if (isPlaying) {
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
        } else {
            stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f)
        }

        mediaSession.setPlaybackState(stateBuilder.build())
        AudioPlayerService.isPlaying = isPlaying
    }

    private fun updateNotification() {
        currentArt?.let { art ->
            val notification = createNotificationBuilder(art, currentAlbumArtUri, isPlaying)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NotificationId, notification.build())

            if (isPlaying) {
                startForeground(NotificationId, notification.build())
            } else {
                stopForeground(false)
            }
        }
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationId)

    }
}
