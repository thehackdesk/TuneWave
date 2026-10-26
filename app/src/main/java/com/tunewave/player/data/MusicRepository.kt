package com.tunewave.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's local music library from MediaStore.
 * No network, no account: everything stays on the device.
 *
 * This must never crash the app: on some devices / Android versions
 * a column (notably ALBUM_ID, deprecated since API 29) is missing
 * from the cursor, and a scan failure used to propagate out of the
 * ViewModel and kill the app right at launch. Every failure here
 * now degrades to an empty library instead.
 */
class MusicRepository(private val context: Context) {

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            scanSongsInternal()
        } catch (e: Exception) {
            Log.w("TuneWave", "Library scan failed, showing empty library", e)
            emptyList()
        }
    }

    private fun scanSongsInternal(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )
        // Only real music files, sorted by title.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            // getColumnIndex (not OrThrow): ALBUM_ID and friends are not
            // guaranteed on every device / Android version.
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val folderColumn = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            if (idColumn < 0 || durationColumn < 0) return emptyList()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val duration = cursor.getLong(durationColumn)
                if (duration <= 0) continue
                val uri = ContentUris.withAppendedId(collection, id)
                songs.add(
                    Song(
                        id = id,
                        title = if (titleColumn >= 0) cursor.getString(titleColumn) ?: "Unknown title" else "Unknown title",
                        artist = if (artistColumn >= 0) cursor.getString(artistColumn) ?: "Unknown artist" else "Unknown artist",
                        album = if (albumColumn >= 0) cursor.getString(albumColumn) ?: "Unknown album" else "Unknown album",
                        albumId = if (albumIdColumn >= 0) cursor.getLong(albumIdColumn) else -1L,
                        durationMs = duration,
                        uri = uri,
                        folder = if (folderColumn >= 0) {
                            cursor.getString(folderColumn)?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: "Unknown"
                        } else {
                            "Unknown"
                        },
                    )
                )
            }
        }
        return songs
    }
}
