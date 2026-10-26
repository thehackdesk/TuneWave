package com.tunewave.player.widget

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.tunewave.player.player.MusicService
import java.util.concurrent.Future

/**
 * Widget button actions.
 *
 * Each action talks to [MusicService] through a MediaController
 * built from its SessionToken, exactly the way PlayerViewModel
 * connects to the service. The controller future is kept and
 * reused across taps so a tap does not create and leak a new
 * controller every time; it is released only when the app's
 * process goes away with it.
 *
 * The service itself refreshes the widget state (see
 * [WidgetUpdater]) when the player's state changes, so the
 * buttons do not try to guess the new state here.
 */
object WidgetControl {

    private var controllerFuture: Future<MediaController>? = null

    fun command(context: Context, action: (MediaController) -> Unit) {
        val appContext = context.applicationContext
        val existing = controllerFuture?.takeIf { !it.isCancelled }
        val future = existing ?: MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, MusicService::class.java)),
        ).buildAsync().also { controllerFuture = it }
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                    ?: return@addListener
                action(controller)
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }
}

class TogglePlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetControl.command(context) { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetControl.command(context) { controller ->
            controller.seekToNextMediaItem()
        }
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetControl.command(context) { controller ->
            controller.seekToPreviousMediaItem()
        }
    }
}
