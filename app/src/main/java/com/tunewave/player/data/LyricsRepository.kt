package com.tunewave.player.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 5: synced lyrics and artist artwork from public web APIs.
 *
 * No API key and no account is needed for either source used here:
 * - Lyrics: LRCLIB (https://lrclib.net), a free lyrics API. The
 *   `/api/get` endpoint matches by artist, track, album and
 *   duration; if it misses, `/api/search` is tried as a fallback.
 * - Artist artwork: Deezer's public search API
 *   (https://api.deezer.com/search/artist), which returns a
 *   picture URL for the first matching artist.
 *
 * Both calls are best-effort: any network error, non-200
 * response, or missing result returns null / an empty list, and
 * the player UI simply shows "lyrics not found". Nothing here
 * touches playback, so a failed fetch can never break a song.
 *
 * Privacy note: these two features are the first online features
 * in TuneWave. They send only the song title, artist, album and
 * duration (for lyrics) or the artist name (for artwork) to those
 * two services, and only when a song is playing. Everything else
 * in the app stays offline and local.
 */
data class LyricLine(val timeMs: Long, val text: String)

data class LyricsResult(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
)

class LyricsRepository {

    suspend fun fetchLyrics(song: Song): LyricsResult? = withContext(Dispatchers.IO) {
        runCatching { fetchLyricsInternal(song) }.getOrNull()
    }

    suspend fun fetchArtistImageUrl(artist: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (artist.isBlank()) return@runCatching null
            val url = "https://api.deezer.com/search/artist?q=" +
                URLEncoder.encode(artist, "UTF-8")
            val body = httpGet(url) ?: return@runCatching null
            val data = JSONObject(body).optJSONArray("data") ?: return@runCatching null
            if (data.length() == 0) return@runCatching null
            data.getJSONObject(0).optString("picture_medium").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun fetchLyricsInternal(song: Song): LyricsResult? {
        val durationSeconds = (song.durationMs / 1000).toInt()
        val getUrl = "https://lrclib.net/api/get?" +
            "artist_name=${URLEncoder.encode(song.artist, "UTF-8")}" +
            "&track_name=${URLEncoder.encode(song.title, "UTF-8")}" +
            "&album_name=${URLEncoder.encode(song.album, "UTF-8")}" +
            "&duration=$durationSeconds"
        httpGet(getUrl)?.let { body ->
            parseLyricsObject(JSONObject(body))?.let { return it }
        }
        // Fallback: search by artist + track, take the first hit
        // that actually has lyrics.
        val searchUrl = "https://lrclib.net/api/search?" +
            "artist_name=${URLEncoder.encode(song.artist, "UTF-8")}" +
            "&track_name=${URLEncoder.encode(song.title, "UTF-8")}"
        val searchBody = httpGet(searchUrl) ?: return null
        val results = JSONArray(searchBody)
        for (i in 0 until results.length()) {
            parseLyricsObject(results.getJSONObject(i))?.let { return it }
        }
        return null
    }

    private fun parseLyricsObject(obj: JSONObject): LyricsResult? {
        val synced = obj.optString("syncedLyrics").takeIf { it.isNotBlank() }
        if (synced != null) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return LyricsResult(lines, isSynced = true)
        }
        val plain = obj.optString("plainLyrics").takeIf { it.isNotBlank() }
        if (plain != null) {
            val lines = plain.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { LyricLine(timeMs = -1L, text = it) }
            if (lines.isNotEmpty()) return LyricsResult(lines, isSynced = false)
        }
        return null
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        // Matches [mm:ss.xx] text and [mm:ss] text. Lines with
        // non-time tags such as [ti:...] or [ar:...] are ignored.
        val pattern = Regex("""\[(\d+):(\d+(?:\.\d+)?)\]\s*(.*)""")
        val lines = mutableListOf<LyricLine>()
        for (rawLine in lrc.lines()) {
            val match = pattern.find(rawLine.trim()) ?: continue
            val minutes = match.groupValues[1].toLongOrNull() ?: continue
            val seconds = match.groupValues[2].toDoubleOrNull() ?: continue
            val text = match.groupValues[3].trim()
            if (text.isEmpty()) continue
            val timeMs = (minutes * 60_000 + seconds * 1000).toLong()
            lines.add(LyricLine(timeMs, text))
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun httpGet(urlString: String): String? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "TuneWave/0.8.0-beta")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
