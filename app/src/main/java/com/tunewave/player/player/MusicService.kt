package com.tunewave.player.player

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tunewave.player.data.PlaylistStore
import com.tunewave.player.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentAudioSessionId: Int = 0

    private val widgetListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            serviceScope.launch {
                WidgetUpdater.update(applicationContext, isPlaying = isPlaying)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val metadata = mediaItem?.mediaMetadata ?: return
            serviceScope.launch {
                WidgetUpdater.update(
                    applicationContext,
                    title = metadata.title?.toString(),
                    artist = metadata.artist?.toString(),
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        currentAudioSessionId = audioManager.generateAudioSessionId()

        val player = ExoPlayer.Builder(this).build().apply {
            setAudioSessionId(currentAudioSessionId)
            addListener(widgetListener)
        }

        mediaSession = MediaSession.Builder(this, player).build()

        val store = PlaylistStore(applicationContext)
        serviceScope.launch {
            combine(store.equalizerEnabled, store.equalizerPreset) { enabled, preset ->
                enabled to preset
            }.collect { (enabled, preset) ->
                if (currentAudioSessionId > 0) {
                    EqualizerManager.apply(currentAudioSessionId, preset, enabled)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        EqualizerManager.release()
        mediaSession?.run {
            player.removeListener(widgetListener)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}