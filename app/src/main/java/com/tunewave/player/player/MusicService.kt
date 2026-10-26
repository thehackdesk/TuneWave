package com.tunewave.player.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tunewave.player.data.PlaylistStore
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

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
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
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
