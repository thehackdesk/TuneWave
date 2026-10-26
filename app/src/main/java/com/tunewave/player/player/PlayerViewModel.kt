package com.tunewave.player.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tunewave.player.data.MusicRepository
import com.tunewave.player.data.Playlist
import com.tunewave.player.data.PlaylistStore
import com.tunewave.player.data.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Phase 1 playback state.
 *
 * The ViewModel owns the MediaController connection to [MusicService].
 * It does not own an ExoPlayer itself: that is the service's job, which
 * is what makes background playback work.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)
    private val playlistStore = PlaylistStore(application.applicationContext)
    private var controllerFuture: java.util.concurrent.Future<MediaController>? = null

    // Phase 2 state, persisted in DataStore (see PlaylistStore).
    val favourites: StateFlow<Set<Long>> = playlistStore.favourites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val playlists: StateFlow<List<Playlist>> = playlistStore.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playCounts: StateFlow<Map<Long, Int>> = playlistStore.playCounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val themeMode: StateFlow<Int> = playlistStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val equalizerEnabled: StateFlow<Boolean> = playlistStore.equalizerEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val equalizerPreset: StateFlow<String> = playlistStore.equalizerPreset
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Flat")
    val crossfadeSeconds: StateFlow<Int> = playlistStore.crossfadeSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    // 0 = off, 1 = all, 2 = one
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            val previousId = _currentSong.value?.id
            _currentSong.value = _songs.value.firstOrNull { it.id == id }
            _positionMs.value = 0L
            // Count a play when the track actually changes to a new song.
            if (id != null && id != previousId) {
                viewModelScope.launch { playlistStore.recordPlay(id) }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> 1
                Player.REPEAT_MODE_ONE -> 2
                else -> 0
            }
        }
    }

    init {
        connect()
        // Poll position while playing so the seek bar moves.
        viewModelScope.launch {
            while (true) {
                val controller = controllerFuture?.let {
                    if (it.isDone) runCatching { it.get() }.getOrNull() else null
                }
                if (controller != null && controller.isPlaying) {
                    _positionMs.value = controller.currentPosition
                }
                delay(500)
            }
        }
    }

    fun connect() {
        val context: Context = getApplication<Application>().applicationContext
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller.addListener(listener)
                _isPlaying.value = controller.isPlaying
                _shuffleEnabled.value = controller.shuffleModeEnabled
                applyPlaylistIfReady(controller)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun loadSongs() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = repository.scanSongs()
                controllerFuture?.let {
                    if (it.isDone) runCatching { it.get() }.getOrNull()?.let { controller ->
                        applyPlaylistIfReady(controller)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyPlaylistIfReady(controller: MediaController) {
        val list = _songs.value
        if (list.isEmpty()) return
        // Only set the playlist once, so a reconnect does not restart playback.
        if (controller.mediaItemCount > 0) return
        controller.setMediaItems(list.map { it.toMediaItem() })
        controller.prepare()
    }

    private fun controllerOrNull(): MediaController? {
        val future = controllerFuture ?: return null
        if (!future.isDone) return null
        return runCatching { future.get() }.getOrNull()
    }

    fun playSong(index: Int) {
        val controller = controllerOrNull() ?: return
        if (controller.mediaItemCount == 0) {
            controller.setMediaItems(_songs.value.map { it.toMediaItem() })
        }
        controller.seekTo(index, 0)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = controllerOrNull() ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun next() {
        controllerOrNull()?.seekToNextMediaItem()
    }

    fun previous() {
        controllerOrNull()?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controllerOrNull()?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun toggleShuffle() {
        val controller = controllerOrNull() ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { playlistStore.setThemeMode(mode) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { playlistStore.setEqualizerEnabled(enabled) }
    }

    fun setEqualizerPreset(preset: String) {
        viewModelScope.launch { playlistStore.setEqualizerPreset(preset) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { playlistStore.setCrossfadeSeconds(seconds) }
    }

    fun toggleFavourite(songId: Long) {
        viewModelScope.launch { playlistStore.toggleFavourite(songId) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistStore.createPlaylist(name) }
    }

    fun addToPlaylist(name: String, songId: Long) {
        viewModelScope.launch { playlistStore.addToPlaylist(name, songId) }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch { playlistStore.deletePlaylist(name) }
    }

    fun cycleRepeat() {
        val controller = controllerOrNull() ?: return
        controller.repeatMode = when (_repeatMode.value) {
            0 -> Player.REPEAT_MODE_ALL
            1 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
