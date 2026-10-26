package com.tunewave.player.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tunewave.player.data.LyricLine
import com.tunewave.player.data.LyricsRepository
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

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)
    private val playlistStore = PlaylistStore(application.applicationContext)
    private val lyricsRepository = LyricsRepository()
    private var controllerFuture: java.util.concurrent.Future<MediaController>? = null

    val favourites: StateFlow<Set<Long>> = playlistStore.favourites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val playlists: StateFlow<List<Playlist>> = playlistStore.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playCounts: StateFlow<Map<Long, Int>> = playlistStore.playCounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val recentlyPlayed: StateFlow<List<Long>> = playlistStore.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val themeMode: StateFlow<Int> = playlistStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val dynamicColor: StateFlow<Boolean> = playlistStore.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
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

    // The queue currently loaded into the player (Daily Mix, an
    // album/artist/playlist list, or the whole library). Shown as
    // "Up next" in the full player.
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // In-player volume (0..1), applied to the Media3 player, not
    // the system volume.
    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun setVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _volume.value = clamped
        controllerOrNull()?.volume = clamped
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _lyricsSynced = MutableStateFlow(false)
    val lyricsSynced: StateFlow<Boolean> = _lyricsSynced.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    private val _artistImageUrl = MutableStateFlow<String?>(null)
    val artistImageUrl: StateFlow<String?> = _artistImageUrl.asStateFlow()

    private var lyricsSongId: Long? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            val previousId = _currentSong.value?.id
            _currentSong.value = _songs.value.firstOrNull { it.id == id }
            _positionMs.value = 0L

            if (id != null && id != previousId) {
                viewModelScope.launch { playlistStore.recordPlay(id) }
            }

            val newSong = _currentSong.value
            if (newSong != null && newSong.id != lyricsSongId) {
                lyricsSongId = newSong.id
                viewModelScope.launch { loadLyricsAndArtwork(newSong) }
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
        viewModelScope.launch {
            while (true) {
                val controller = controllerOrNull()
                if (controller != null && controller.isPlaying) {
                    _positionMs.value = controller.currentPosition
                }
                delay(500)
            }
        }
    }

    fun connect() {
        runCatching {
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
                    _repeatMode.value = when (controller.repeatMode) {
                        Player.REPEAT_MODE_ALL -> 1
                        Player.REPEAT_MODE_ONE -> 2
                        else -> 0
                    }

                    syncActiveSong(controller)
                    applyPlaylistIfReady(controller)
                },
                ContextCompat.getMainExecutor(context),
            )
        }.onFailure { Log.w("TuneWave", "MediaController connect failed", it) }
    }

    private fun syncActiveSong(controller: MediaController) {
        val currentId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        if (currentId != null) {
            val foundSong = _songs.value.firstOrNull { it.id == currentId }
            if (foundSong != null) {
                _currentSong.value = foundSong
                _positionMs.value = controller.currentPosition
                if (lyricsSongId != foundSong.id) {
                    lyricsSongId = foundSong.id
                    viewModelScope.launch { loadLyricsAndArtwork(foundSong) }
                }
            }
        }
    }

    private suspend fun loadLyricsAndArtwork(song: Song) {
        _lyricsLoading.value = true
        try {
            val result = lyricsRepository.fetchLyrics(song)
            _lyrics.value = result?.lines ?: emptyList()
            _lyricsSynced.value = result?.isSynced ?: false
            _artistImageUrl.value = lyricsRepository.fetchArtistImageUrl(song.artist)
        } finally {
            _lyricsLoading.value = false
        }
    }

    fun loadSongs() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = repository.scanSongs()
                controllerOrNull()?.let { controller ->
                    syncActiveSong(controller)
                    applyPlaylistIfReady(controller)
                }
            } catch (e: Exception) {
                Log.w("TuneWave", "loadSongs failed", e)
                _songs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyPlaylistIfReady(controller: MediaController) {
        val list = _songs.value
        if (list.isEmpty()) return
        if (controller.mediaItemCount > 0) return
        controller.setMediaItems(list.map { it.toMediaItem() })
        controller.prepare()
        _queue.value = list
    }

    private fun controllerOrNull(): MediaController? {
        val future = controllerFuture ?: return null
        if (!future.isDone) return null
        return runCatching { future.get() }.getOrNull()
    }

    fun playDailyMix() {
        val controller = controllerOrNull() ?: return
        val all = _songs.value
        if (all.isEmpty()) return
        val seed = java.time.LocalDate.now().toEpochDay()
        val random = kotlin.random.Random(seed)
        val favs = all.filter { it.id in favourites.value }
        val others = (all - favs.toSet()).shuffled(random)
        val mix = (favs.shuffled(random) + others).take(25)
        if (mix.isEmpty()) return

        controller.setMediaItems(mix.map { it.toMediaItem() }, 0, 0)
        controller.prepare()
        controller.play()
        _queue.value = mix
    }

    fun playSong(song: Song, queue: List<Song>? = null) {
        val controller = controllerOrNull() ?: return
        val targetQueue = if (queue.isNullOrEmpty()) _songs.value else queue
        val index = targetQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        controller.setMediaItems(targetQueue.map { it.toMediaItem() }, index, 0)
        controller.prepare()
        controller.play()
        _queue.value = targetQueue
    }

    fun playSong(index: Int) {
        val all = _songs.value
        if (index in all.indices) {
            playSong(all[index], all)
        }
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

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { playlistStore.setDynamicColor(enabled) }
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

    fun renamePlaylist(oldName: String, newName: String) {
        viewModelScope.launch { playlistStore.renamePlaylist(oldName, newName) }
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