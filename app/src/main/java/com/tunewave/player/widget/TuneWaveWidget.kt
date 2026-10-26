package com.tunewave.player.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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

class TuneWaveWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val openAppIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
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
                        onClick = actionStartActivity(openAppIntent),
                    )
                }
            }
        }
    }
}

object WidgetKeys {
    val TITLE = stringPreferencesKey("widget_title")
    val ARTIST = stringPreferencesKey("widget_artist")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
}