package com.tunewave.player.player

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Phase 4 equalizer, using Android's built-in audio effects.
 *
 * Preset levels are in millibels, for up to 5 bands. Devices report
 * their own band count and level range, so levels are clamped and
 * only as many bands as the device has are set.
 *
 * This applies to the ExoPlayer audio session owned by [MusicService].
 * It is not a replacement for FFmpeg or TagLib, which are not included
 * in this build: formats are therefore limited to what the device's
 * own decoders support, and ExoPlayer already plays gaplessly by
 * default for consecutive tracks of the same format.
 */
object EqualizerManager {

    val presets: Map<String, List<Int>> = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(500, 300, 0, -200, -300),
        "Treble Boost" to listOf(-300, -200, 0, 300, 500),
        "Vocal" to listOf(-200, 0, 300, 300, 0),
        "Rock" to listOf(400, 200, 0, 200, 400),
        "Pop" to listOf(200, 300, 300, 200, 0),
    )

    private var equalizer: Equalizer? = null

    fun apply(audioSessionId: Int, presetName: String, enabled: Boolean) {
        try {
            equalizer?.release()
            equalizer = null
            if (!enabled || audioSessionId == 0) return
            val eq = Equalizer(0, audioSessionId)
            val levels = presets[presetName] ?: presets["Flat"]!!
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            for (band in 0 until minOf(bandCount, levels.size)) {
                val level = levels[band].toShort().coerceIn(range[0], range[1])
                eq.setBandLevel(band.toShort(), level)
            }
            eq.enabled = true
            equalizer = eq
        } catch (e: Exception) {
            // Equalizer is device-dependent: some devices / sessions reject it.
            // Playback must keep working even when the effect cannot be applied.
            Log.w("TuneWave", "Equalizer could not be applied", e)
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
            // Best effort only.
        }
        equalizer = null
    }
}
