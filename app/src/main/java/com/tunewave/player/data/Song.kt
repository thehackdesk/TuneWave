package com.tunewave.player.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * One local audio file, as read from MediaStore.
 * [uri] is a content:// URI that ExoPlayer can play directly.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
) {
    // Album art from MediaStore. May be null for albums without art.
    val albumArtUri: Uri?
        get() = if (albumId > 0) {
            Uri.parse("content://media/external/audio/albumart/$albumId")
        } else {
            null
        }

    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(metadata)
            .build()
    }
}
