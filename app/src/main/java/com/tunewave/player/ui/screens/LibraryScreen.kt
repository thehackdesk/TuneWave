package com.tunewave.player.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tunewave.player.data.Playlist
import com.tunewave.player.data.Song
import com.tunewave.player.player.PlayerViewModel

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

// Sub-sections under the Library bottom-nav destination.
private val LIB_TITLES = listOf("Albums", "Artists", "Playlists", "Folders", "Stats")

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
    val positionMs by viewModel.positionMs.collectAsState()
    val favourites by viewModel.favourites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()

    // Bottom navigation: 0 = Home, 1 = Songs, 2 = Liked, 3 = Library.
    // libIndex selects the Library sub-section (LIB_TITLES order).
    var navIndex by remember { mutableStateOf(0) }
    var libIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(0) }
    var favouritesOnly by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }

    val clearSelections = {
        selectedAlbum = null
        selectedArtist = null
        selectedPlaylist = null
        selectedFolder = null
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TuneWave") },
                actions = {
                    TextButton(onClick = { viewModel.playDailyMix() }) {
                        Text("Daily Mix")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                if (currentSong != null) {
                    val duration = currentSong!!.durationMs
                    val progress = if (duration > 0) {
                        (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    MiniPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = progress,
                        onToggle = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.previous() },
                        onNext = { viewModel.next() },
                        onOpenPlayer = onOpenPlayer,
                    )
                }
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = navIndex == 0,
                        onClick = { navIndex = 0; clearSelections() },
                        icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = navIndex == 1,
                        onClick = { navIndex = 1; clearSelections() },
                        icon = { Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null) },
                        label = { Text("Songs") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = navIndex == 2,
                        onClick = { navIndex = 2; clearSelections() },
                        icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text("Liked") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                    NavigationBarItem(
                        selected = navIndex == 3,
                        onClick = { navIndex = 3; clearSelections() },
                        icon = { Icon(imageVector = Icons.Filled.List, contentDescription = null) },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (navIndex == 3) {
                ScrollableTabRow(selectedTabIndex = libIndex, edgePadding = 16.dp) {
                    LIB_TITLES.forEachIndexed { index, title ->
                        Tab(
                            selected = libIndex == index,
                            onClick = {
                                libIndex = index
                                clearSelections()
                            },
                            text = { Text(title) },
                        )
                    }
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

            if (navIndex == 1 || selectedAlbum != null || selectedArtist != null || selectedPlaylist != null || selectedFolder != null) {
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
                    val albumSongs = filteredSongs.filter { it.album == selectedAlbum }
                    DetailHeader(title = selectedAlbum!!, onBack = { selectedAlbum = null })
                    SongList(
                        list = albumSongs,
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { viewModel.playSong(it, albumSongs) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                selectedArtist != null -> {
                    val artistSongs = filteredSongs.filter { it.artist == selectedArtist }
                    DetailHeader(title = selectedArtist!!, onBack = { selectedArtist = null })
                    SongList(
                        list = artistSongs,
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { viewModel.playSong(it, artistSongs) },
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
                        onPlay = { viewModel.playSong(it, playlistSongs) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                selectedFolder != null -> {
                    val folderSongs = filteredSongs.filter { it.folder == selectedFolder }
                    DetailHeader(title = selectedFolder!!, onBack = { selectedFolder = null })
                    SongList(
                        list = folderSongs,
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { viewModel.playSong(it, folderSongs) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                navIndex == 0 -> {
                    HomeContent(
                        songs = songs,
                        favourites = favourites,
                        recentlyPlayed = recentlyPlayed,
                        currentSong = currentSong,
                        onPlay = { song, queue -> viewModel.playSong(song, queue) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                        onSurpriseMe = { viewModel.playDailyMix() },
                        onShowSongs = { navIndex = 1 },
                        onShowLiked = { navIndex = 2 },
                        onShowAlbums = { navIndex = 3; libIndex = 0 },
                        onSelectAlbum = { selectedAlbum = it },
                    )
                }
                navIndex == 1 -> {
                    SongList(
                        list = filteredSongs,
                        currentSong = currentSong,
                        favourites = favourites,
                        onPlay = { viewModel.playSong(it, filteredSongs) },
                        onFavourite = { viewModel.toggleFavourite(it.id) },
                        onAddToPlaylist = { songForPlaylist = it },
                    )
                }
                navIndex == 2 -> {
                    val likedSongs = filteredSongs.filter { it.id in favourites }
                    if (likedSongs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Ekono kono favourite nai", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gaan-er heart icon-e tap korlei ekhane asbe.")
                        }
                    } else {
                        SongList(
                            list = likedSongs,
                            currentSong = currentSong,
                            favourites = favourites,
                            onPlay = { viewModel.playSong(it, likedSongs) },
                            onFavourite = { viewModel.toggleFavourite(it.id) },
                            onAddToPlaylist = { songForPlaylist = it },
                        )
                    }
                }
                navIndex == 3 && libIndex == 0 -> {
                    val albums = songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(albums.chunked(2)) { rowAlbums ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                rowAlbums.forEach { (album, albumSongs) ->
                                    AlbumGridCard(
                                        album = album,
                                        albumSongs = albumSongs,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedAlbum = album },
                                    )
                                }
                                if (rowAlbums.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                navIndex == 3 && libIndex == 1 -> {
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
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    val art = artistSongs.firstOrNull()?.albumArtUri
                                    if (art != null) {
                                        AsyncImage(
                                            model = art,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
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
                navIndex == 3 && libIndex == 2 -> {
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
                                    IconButton(onClick = {
                                        playlistToRename = playlist
                                        renameText = playlist.name
                                    }) {
                                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename playlist")
                                    }
                                    IconButton(onClick = { viewModel.deletePlaylist(playlist.name) }) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete playlist")
                                    }
                                }
                            }
                        }
                    }
                }
                navIndex == 3 && libIndex == 3 -> {
                    val folders = songs.groupBy { it.folder }.toList().sortedBy { it.first.lowercase() }
                    if (folders.size == 1 && folders[0].first == "Unknown") {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Folder dekha jacche na", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Android-er scoped storage-er jonno ei phone MediaStore theke asol folder path dicche na, " +
                                    "tai gaan-gulo folder hisebe vag kora jacche na. Songs/Albums/Artists tab theke shob gaan pawa jabe.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(folders) { (folder, folderSongs) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFolder = folder }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${folderSongs.size} gaan",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                navIndex == 3 && libIndex == 4 -> {
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

        val playlistPendingRename = playlistToRename
        if (playlistPendingRename != null) {
            AlertDialog(
                onDismissRequest = { playlistToRename = null },
                title = { Text("Playlist rename koro") },
                text = {
                    TextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        placeholder = { Text("Notun naam") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renamePlaylist(playlistPendingRename.name, renameText)
                        playlistToRename = null
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToRename = null }) { Text("Cancel") }
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

/**
 * Home tab, adapted from the charcoal/lime/violet web prototype
 * the user supplied: a headline, a gradient hero card with a
 * "Play a mix" action, then Recently played (real recency,
 * tracked in DataStore), Favourites, and an Albums strip.
 */
@Composable
private fun HomeContent(
    songs: List<Song>,
    favourites: Set<Long>,
    recentlyPlayed: List<Long>,
    currentSong: Song?,
    onPlay: (Song, List<Song>) -> Unit,
    onFavourite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onSurpriseMe: () -> Unit,
    onShowSongs: () -> Unit,
    onShowLiked: () -> Unit,
    onShowAlbums: () -> Unit,
    onSelectAlbum: (String) -> Unit,
) {
    val recentSongs = remember(songs, recentlyPlayed) {
        recentlyPlayed.mapNotNull { id -> songs.firstOrNull { it.id == id } }
    }
    val favouriteSongs = remember(songs, favourites) {
        songs.filter { it.id in favourites }
    }
    val albums = remember(songs) {
        songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "YOUR PERSONAL SOUNDTRACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("Feel every", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "frequency.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "A little rhythm can change everything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF272B40), Color(0xFF31314A)),
                        )
                    )
                    .padding(20.dp),
            ) {
                Column {
                    Text(
                        text = "TUNEWAVE MIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDED8FF),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your vibe. On repeat.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    Text(
                        text = "A fresh mix from your library, every day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD1CEDE),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSurpriseMe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB6F36B),
                            contentColor = Color(0xFF202617),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play a mix")
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Recently played",
                subtitle = "Ja tumi shesh shunecho",
                onShowAll = onShowSongs,
            )
        }
        if (recentSongs.isEmpty()) {
            item {
                Text(
                    text = "Ekono kono gaan play koro nai — play korlei ekhane dekhabe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        } else {
            item {
                Column {
                    recentSongs.take(6).forEach { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSong?.id,
                            isFavourite = song.id in favourites,
                            onClick = { onPlay(song, recentSongs) },
                            onFavourite = { onFavourite(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Favourites",
                subtitle = "Tomar pochonder gaan",
                onShowAll = onShowLiked,
            )
        }
        if (favouriteSongs.isEmpty()) {
            item {
                Text(
                    text = "Ekono kono favourite nai — gaan-er heart-e tap korlei ekhane asbe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        } else {
            item {
                Column {
                    favouriteSongs.take(6).forEach { song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSong?.id,
                            isFavourite = true,
                            onClick = { onPlay(song, favouriteSongs) },
                            onFavourite = { onFavourite(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Albums",
                subtitle = "Tomar collection theke",
                onShowAll = onShowAlbums,
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(albums) { (album, albumSongs) ->
                    AlbumGridCard(
                        album = album,
                        albumSongs = albumSongs,
                        modifier = Modifier.width(140.dp),
                        onClick = { onSelectAlbum(album) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, onShowAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onShowAll) { Text("Show all") }
    }
}

/**
 * Album card for the Home strip and the Albums grid: square art on
 * top, name / artist / count below, like the prototype.
 */
@Composable
private fun AlbumGridCard(
    album: String,
    albumSongs: List<Song>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(19.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(45.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val art = albumSongs.firstOrNull()?.albumArtUri
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = album,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${albumSongs.firstOrNull()?.artist ?: ""} • ${albumSongs.size} gaan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

/**
 * Album art thumbnail with a music-note fallback underneath: if the
 * art fails to load (or there is none), the note still shows instead
 * of an empty box.
 */
@Composable
private fun AlbumArt(artUri: Uri?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (artUri != null) {
            AsyncImage(
                model = artUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
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
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(artUri = song.albumArtUri, size = 57.dp)
        Spacer(modifier = Modifier.width(13.dp))
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

/**
 * Floating mini player card, like the prototype's mobile player:
 * dark charcoal card with a subtle border, previous / play / next
 * controls, and a thin lime progress bar along the bottom.
 */
@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color(0xFF20202B))
            .border(BorderStroke(1.dp, Color(0xFF3A3947)), RoundedCornerShape(19.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(artUri = song.albumArtUri, size = 47.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA09EAF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(39.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222230))
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color(0xFFC6C3D1),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(39.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB6F36B))
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF202617),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(39.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222230))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color(0xFFC6C3D1),
                )
            }
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = Color(0xFFB6F36B),
            trackColor = Color(0xFF222230),
        )
    }
}