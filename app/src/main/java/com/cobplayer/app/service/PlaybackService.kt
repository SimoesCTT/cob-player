package com.cobplayer.app.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.cobplayer.app.audio.EqHolder
import com.cobplayer.app.audio.EqualizerController
import com.cobplayer.app.net.CobDataSourceFactory
import com.cobplayer.app.ui.MainActivity
import com.google.android.gms.cast.framework.CastContext

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null
    private var currentMediaItem: MediaItem? = null
    var equalizer: EqualizerController = EqualizerController()

    override fun onCreate() {
        super.onCreate()

        val dataSourceFactory = CobDataSourceFactory(applicationContext)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // NextLib FFmpeg renderers for AC3/DTS/TrueHD/ALAC support
        val renderersFactory: androidx.media3.exoplayer.RenderersFactory =
            DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)

        val exo = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exo.addAnalyticsListener(object :
            androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                equalizer.attach(audioSessionId)
                EqHolder.controller = equalizer
            }
        })

        exoPlayer = exo

        val activePlayer: Player = try {
            val castContext = CastContext.getSharedInstance(this)
            val cp = CastPlayer(castContext)
            cp.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                override fun onCastSessionAvailable() {
                    currentMediaItem?.let {
                        cp.setMediaItem(it, exo.currentPosition)
                        cp.prepare()
                        cp.playWhenReady = true
                    }
                    mediaSession?.player = cp
                }
                override fun onCastSessionUnavailable() {
                    val pos = cp.currentPosition
                    currentMediaItem?.let {
                        exo.setMediaItem(it, pos)
                        exo.prepare()
                        exo.playWhenReady = cp.playWhenReady
                    }
                    mediaSession?.player = exo
                }
            })
            castPlayer = cp
            exo
        } catch (e: Exception) {
            exo
        }

        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, activePlayer)
            .setSessionActivity(sessionActivity)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): com.google.common.util.concurrent.ListenableFuture<List<MediaItem>> {
                    mediaItems.firstOrNull()?.let { currentMediaItem = it }
                    return com.google.common.util.concurrent.Futures.immediateFuture(mediaItems)
                }
            })
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady ||
            p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED
        ) stopSelf()
    }

    override fun onDestroy() {
        EqHolder.controller = null
        equalizer.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        castPlayer = null
        exoPlayer = null
        super.onDestroy()
    }
}
