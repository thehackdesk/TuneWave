package com.tunewave.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.tunewave.player.player.PlayerViewModel
import com.tunewave.player.ui.screens.LibraryScreen
import com.tunewave.player.ui.screens.PlayerScreen
import com.tunewave.player.ui.screens.SettingsScreen
import com.tunewave.player.ui.theme.TuneWaveTheme

/**
 * Phase 1 entry point: library + player, backed by [PlayerViewModel]
 * and the background [com.tunewave.player.player.MusicService].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private var showPlayer by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.loadSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadSongs()
        } else {
            permissionLauncher.launch(audioPermission)
        }
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }
            TuneWaveTheme(darkTheme = darkTheme) {
                when {
                    showSettings -> {
                        SettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
                    }
                    showPlayer -> {
                        PlayerScreen(viewModel = viewModel, onBack = { showPlayer = false })
                    }
                    else -> {
                        LibraryScreen(
                            viewModel = viewModel,
                            onOpenPlayer = { showPlayer = true },
                            onRefresh = { viewModel.loadSongs() },
                            onOpenSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}
