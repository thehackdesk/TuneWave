package com.tunewave.player.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunewave.player.MainActivity

/**
 * Phase 3 home screen widget, built with Glance.
 *
 * The widget shows the current (or last played) song and three
 * controls: previous, play/pause, next. Tapping the song title
 * opens the app. The widget's own state lives in Glance
 * Preferences, not in the app's DataStore, and is refreshed by
 * [WidgetUpdater] from [com.tunewave.player.player.MusicService],
 * which owns the player and therefore sees track and play-state
 * changes even when the app UI is closed.
 *
 * Album art is not shown: Glance image loading from a MediaStore
 * content URI needs extra plumbing, and this first widget version
 * keeps to text + controls so it stays reliable. The widget has
 * not been tested on a device yet, like the rest of the app, so
 * the Glance API usage here should be confirmed at the first
 * Android Studio build.
 */
class TuneWaveWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[WidgetKeys.TITLE] ?: "TuneWave"
            val artist = prefs[WidgetKeys.ARTIST] ?: "No song playing yet"
            val isPlaying = prefs[WidgetKeys.IS_PLAYING] ?: false

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ColorProvider(Color.Black),
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                Text(
                    text = artist,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(Color.DarkGray),
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                Spacer(modifier = GlanceModifier.height(12.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Button(
                        text = "⏮",
                        onClick = actionRunCallback<PreviousAction>(),
                    )
                    Button(
                        text = if (isPlaying) "⏸" else "▶",
                        onClick = actionRunCallback<TogglePlayPauseAction>(),
                    )
                    Button(
                        text = "⏭",
                        onClick = actionRunCallback<NextAction>(),
                    )
                    Button(
                        text = "Open",
                        onClick = actionStartActivity<MainActivity>(),
                    )
                }
            }
        }
    }
}

/** Glance Preferences keys for the widget's own state. */
object WidgetKeys {
    val TITLE = stringPreferencesKey("widget_title")
    val ARTIST = stringPreferencesKey("widget_artist")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
}
