package com.tunewave.player.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * Refreshes every placed TuneWave widget with the latest track info.
 *
 * Only the arguments that are not null are overwritten, so a
 * play-state-only update does not erase the song title, and a
 * track-only update does not erase the play state. If no widget
 * is placed on the home screen, this is a no-op.
 */
object WidgetUpdater {

    suspend fun update(
        context: Context,
        title: String? = null,
        artist: String? = null,
        isPlaying: Boolean? = null,
    ) {
        val glanceIds = GlanceAppWidgetManager(context)
            .getGlanceIds(TuneWaveWidget::class.java)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs ->
                if (title != null) prefs[WidgetKeys.TITLE] = title
                if (artist != null) prefs[WidgetKeys.ARTIST] = artist
                if (isPlaying != null) prefs[WidgetKeys.IS_PLAYING] = isPlaying
            }
            TuneWaveWidget().update(context, glanceId)
        }
    }
}
