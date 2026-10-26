package com.tunewave.player.player

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Background playback service.
 *
 * MediaSessionService keeps playback alive when the app UI is in the
 * background and drives the system notification / media controls.
 * The UI talks to this service through a MediaController, never
 * directly to the player, so the service owns the single ExoPlayer.
 *
 * Phase 4: when the service starts, the saved equalizer preset is
 * applied to this player's audio session. Changing the preset in
 * Settings takes effect on the next playback start, not mid-track,
 * because the effect is bound to the session created here.
 */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Phase 3 widget: the service owns the player, so it is the
    // one place that sees track and play-state changes even when
    // the app UI is closed. It pushes those changes to the widget.
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
        val player = ExoPlayer.Builder(this).build()
        player.addListener(widgetListener)
        mediaSession = MediaSession.Builder(this, player).build()
        val store = PlaylistStore(applicationContext)
        serviceScope.launch {
            val enabled = store.equalizerEnabled.first()
            val preset = store.equalizerPreset.first()
            EqualizerManager.apply(player.audioSessionId, preset, enabled)
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
