package com.tunewave.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tunewave_prefs")

data class Playlist(val name: String, val songIds: List<Long>)

/**
 * Phase 2 persistence, using DataStore Preferences (no server, no account).
 *
 * Playlists are stored as a simple text format:
 *   name::id,id;;name2::id
 * Playlist names therefore cannot contain "::" or ";;" in this MVP.
 * Favourites and play counts are comma / colon separated id lists.
 * This is deliberately simple for Phase 2; Room can replace it later
 * without changing the ViewModel API.
 */
class PlaylistStore(private val context: Context) {

    private val favouriteKey = stringPreferencesKey("favourite_ids")
    private val playlistsKey = stringPreferencesKey("playlists")
    private val playsKey = stringPreferencesKey("play_counts")
    private val recentKey = stringPreferencesKey("recently_played_ids")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = stringPreferencesKey("dynamic_color")
    private val equalizerEnabledKey = stringPreferencesKey("equalizer_enabled")
    private val equalizerPresetKey = stringPreferencesKey("equalizer_preset")
    private val crossfadeKey = stringPreferencesKey("crossfade_seconds")

    // Phase 3 theme setting: 0 = system, 1 = light, 2 = dark.
    // Every flow has a .catch default: a corrupt DataStore file must
    // never crash launch, it just resets that setting to its default.
    val themeMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.toIntOrNull() ?: 0
    }.catch { emit(0) }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.toString()
        }
    }

    // Material You dynamic colour: off by default so TuneWave's own
    // deep-blue palette shows; toggleable in Settings.
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[dynamicColorKey]?.toBooleanStrictOrNull() ?: false
    }.catch { emit(false) }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[dynamicColorKey] = enabled.toString()
        }
    }

    // Phase 4 audio settings.
    val equalizerEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[equalizerEnabledKey]?.toBooleanStrictOrNull() ?: false
    }.catch { emit(false) }

    val equalizerPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[equalizerPresetKey] ?: "Flat"
    }.catch { emit("Flat") }

    val crossfadeSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[crossfadeKey]?.toIntOrNull() ?: 0
    }.catch { emit(0) }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[equalizerEnabledKey] = enabled.toString()
        }
    }

    suspend fun setEqualizerPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[equalizerPresetKey] = preset
        }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[crossfadeKey] = seconds.toString()
        }
    }

    val favourites: Flow<Set<Long>> = context.dataStore.data.map { prefs ->
        prefs[favouriteKey]
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }.catch { emit(emptySet()) }

    val playlists: Flow<List<Playlist>> = context.dataStore.data.map { prefs ->
        parsePlaylists(prefs[playlistsKey])
    }.catch { emit(emptyList()) }

    // Most recently played song ids, most recent first, max 20.
    // Updated together with play counts whenever a song starts.
    val recentlyPlayed: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        prefs[recentKey]
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
    }.catch { emit(emptyList()) }

    val playCounts: Flow<Map<Long, Int>> = context.dataStore.data.map { prefs ->
        prefs[playsKey]
            ?.split(",")
            ?.mapNotNull { part ->
                val bits = part.split(":")
                if (bits.size == 2) {
                    val id = bits[0].toLongOrNull()
                    val count = bits[1].toIntOrNull()
                    if (id != null && count != null) id to count else null
                } else {
                    null
                }
            }
            ?.toMap()
            ?: emptyMap()
    }.catch { emit(emptyMap()) }

    private fun parsePlaylists(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";;").mapNotNull { entry ->
            val bits = entry.split("::")
            if (bits.size != 2) {
                null
            } else {
                Playlist(
                    name = bits[0],
                    songIds = bits[1].split(",").mapNotNull { it.toLongOrNull() },
                )
            }
        }
    }

    private fun encodePlaylists(list: List<Playlist>): String {
        return list.joinToString(";;") { playlist ->
            "${playlist.name}::${playlist.songIds.joinToString(",")}"
        }
    }

    suspend fun toggleFavourite(songId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[favouriteKey]
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
            val next = if (songId in current) current - songId else current + songId
            prefs[favouriteKey] = next.joinToString(",")
        }
    }

    suspend fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[playlistsKey])
            if (current.none { it.name == cleanName }) {
                prefs[playlistsKey] = encodePlaylists(current + Playlist(cleanName, emptyList()))
            }
        }
    }

    suspend fun addToPlaylist(name: String, songId: Long) {
        context.dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[playlistsKey])
            val next = current.map { playlist ->
                if (playlist.name == name && songId !in playlist.songIds) {
                    playlist.copy(songIds = playlist.songIds + songId)
                } else {
                    playlist
                }
            }
            prefs[playlistsKey] = encodePlaylists(next)
        }
    }

    suspend fun deletePlaylist(name: String) {
        context.dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[playlistsKey])
            prefs[playlistsKey] = encodePlaylists(current.filter { it.name != name })
        }
    }

    suspend fun renamePlaylist(oldName: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty() || cleanName == oldName) return
        context.dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[playlistsKey])
            if (current.none { it.name == cleanName }) {
                prefs[playlistsKey] = encodePlaylists(
                    current.map { playlist ->
                        if (playlist.name == oldName) playlist.copy(name = cleanName) else playlist
                    }
                )
            }
        }
    }

    suspend fun recordPlay(songId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[playsKey]
                ?.split(",")
                ?.mapNotNull { part ->
                    val bits = part.split(":")
                    if (bits.size == 2) {
                        val id = bits[0].toLongOrNull()
                        val count = bits[1].toIntOrNull()
                        if (id != null && count != null) id to count else null
                    } else {
                        null
                    }
                }
                ?.toMap()
                ?: emptyMap()
            val next = current.toMutableMap()
            next[songId] = (next[songId] ?: 0) + 1
            prefs[playsKey] = next.entries.joinToString(",") { "${it.key}:${it.value}" }
            val recent = prefs[recentKey]
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()
            prefs[recentKey] = (listOf(songId) + recent.filter { it != songId })
                .take(20)
                .joinToString(",")
        }
    }
}
