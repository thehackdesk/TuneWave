package com.tunewave.player.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The system-facing receiver for the TuneWave home screen widget.
 * Glance handles widget lifecycle through this receiver; the
 * widget itself is [TuneWaveWidget].
 */
class TuneWaveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TuneWaveWidget()
}
