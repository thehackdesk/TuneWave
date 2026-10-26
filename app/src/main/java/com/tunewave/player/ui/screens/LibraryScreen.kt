package com.tunewave.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunewave.player.data.Playlist
import com.tunewave.player.data.Song
import com.tunewave.player.player.PlayerViewModel

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

private val TAB_TITLES = listOf("Songs", "Albums", "Artists", "Playlists", "Stats")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onOpenPlayer: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val favourites by viewModel.favourites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(0) } // 0 title, 1 artist, 2 duration
    var favouritesOnly by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    val searched = songs.filter { song ->
        val q = searchQuery.trim()
        (q.isEmpty() || song.title.contains(q, ignoreCase = true) ||
            song.artist.contains(q, ignoreCase = true) ||
            song.album.contains(q, ignoreCase = true)) &&
            (!favouritesOnly || song.id in favourites)
    }
    val filteredSongs = when (sortMode) {
        1 -> searched.sortedBy { it.artist.lowercase() }
        2 -> searched.sortedByDescending { it.durationMs }
        else -> searched.sortedBy { it.title.lowercase() }
    }

    fun playSong(song: Song) {
        val originalIndex = songs.indexOfFirst { it.id == song.id }
        if (originalIndex >= 0) viewModel.playSong(originalIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TuneWave") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            if (currentSong != null) {
                MiniPlayer(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    onToggle = { viewModel.togglePlayPause() },
                    onNext = { viewModel.next() },
                    onOpenPlayer = onOpenPlayer,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                            selectedAlbum = null
                            selectedArtist = null
                            selectedPlaylist = null
                        },
                        text = { Text(title) },
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (songs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Kono gaan pawa jayni", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phone-e gaan thakle audio permission dao, tarpor abar try koro.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRefresh) { Text("Abar scan koro") }
                }
                return@Column
            }

            // Search + sort + favourites filter, shown on Songs tab and group detail lists.
            if (tabIndex == 0 || selectedAlbum != null || selectedArtist != null || selectedPlaylist != null) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search gaan, artist, album") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FilterChip(
                        selected = favouritesOnly,
                        onClick = { favouritesOnly = !favouritesOnly },
                        label = { Text("Favourites") },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { sortMode = 0 }) { Text(if (sortMode == 0) "• Title" else "Title") }
                    TextButton(onClick = { sortMode = 1 }) { Text(if (sortMode == 1) "• Artist" else "Artist") }
                    TextButton(onClick = { sortMode = 2 }) { Text(if (sortMode == 2) "• Longest" else "Longest") }
                }
            }

            when {
                selectedAlbum != null -> {
                    DetailHeader(title = selectedAlbum!!, onBack = { selectedAlbum = null })
                    SongList(
                        list = filteredSongs.filter { it.album == selectedAlbum },
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { playSong(it) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                selectedArtist != null -> {
                    DetailHeader(title = selectedArtist!!, onBack = { selectedArtist = null })
                    SongList(
                        list = filteredSongs.filter { it.artist == selectedArtist },
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { playSong(it) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                selectedPlaylist != null -> {
                    val playlist = selectedPlaylist!!
                    DetailHeader(title = playlist.name, onBack = { selectedPlaylist = null })
                    val playlistSongs = playlist.songIds.mapNotNull { id ->
                        songs.firstOrNull { it.id == id }
                    }
                    SongList(
                        list = playlistSongs,
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { playSong(it) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                tabIndex == 0 -> {
                    SongList(
                        list = filteredSongs,
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { playSong(it) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                tabIndex == 1 -> {
                    val albums = songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(albums) { (album, albumSongs) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAlbum = album }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(album, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${albumSongs.firstOrNull()?.artist ?: ""} • ${albumSongs.size} gaan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                tabIndex == 2 -> {
                    val artists = songs.groupBy { it.artist }.toList().sortedBy { it.first.lowercase() }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(artists) { (artist, artistSongs) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedArtist = artist }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${artistSongs.size} gaan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                tabIndex == 3 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notun playlist")
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPlaylist = playlist }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${playlist.songIds.size} gaan",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deletePlaylist(playlist.name) }) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete playlist")
                                    }
                                }
                            }
                        }
                    }
                }
                tabIndex == 4 -> {
                    val totalPlays = playCounts.values.sum()
                    val mostPlayed = playCounts.maxByOrNull { it.value }
                        ?.let { entry -> songs.firstOrNull { it.id == entry.key } to entry.value }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total gaan: ${songs.size}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total somoy: ${formatDuration(songs.sumOf { it.durationMs })}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total play: $totalPlays", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Favourites: ${favourites.size}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Playlists: ${playlists.size}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (mostPlayed?.first != null) {
                            Text("Sobcheye beshi shona: ${mostPlayed.first!!.title} (${mostPlayed.second} bar)", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text("Ekono kono gaan play kora hoyni.", style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Note: Folder diye browse ekhono add hoyni. Android-er scoped storage-e gaaner folder path shob somoy pawa jay na, tai Phase 2-te Album/Artist diye organization deoa hoyeche.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Notun playlist") },
                text = {
                    TextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist-er naam") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreateDialog = false
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                },
            )
        }

        val pendingSong = songForPlaylist
        if (pendingSong != null) {
            AlertDialog(
                onDismissRequest = { songForPlaylist = null },
                title = { Text("Playlist-e add koro") },
                text = {
                    if (playlists.isEmpty()) {
                        Text("Age ekta playlist banao.")
                    } else {
                        Column {
                            playlists.forEach { playlist ->
                                TextButton(onClick = {
                                    viewModel.addToPlaylist(playlist.name, pendingSong.id)
                                    songForPlaylist = null
                                }) { Text(playlist.name) }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { songForPlaylist = null }) { Text("Close") }
                },
            )
        }
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SongList(
    list: List<Song>,
    currentSong: Song?,
    favourites: Set<Long>,
    onPlay: (Song) -> Unit,
    onFavourite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(list) { song ->
            SongRow(
                song = song,
                isCurrent = song.id == currentSong?.id,
                isFavourite = song.id in favourites,
                onClick = { onPlay(song) },
                onFavourite = { onFavourite(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onFavourite: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavourite) {
            Icon(
                imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favourite",
                tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(imageVector = Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist")
        }
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
            )
        }
        IconButton(onClick = onNext) {
            Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
        }
    }
}
