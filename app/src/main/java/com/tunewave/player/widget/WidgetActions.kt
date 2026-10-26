package com.tunewave.player.widget

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tunewave.player.player.MusicService
import kotlinx.coroutines.suspendCancellableCoroutine

object WidgetControl {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    suspend fun command(context: Context, action: (MediaController) -> Unit) {
        val appContext = context.applicationContext
        val existing = controllerFuture?.takeIf { !it.isCancelled }
        val future = existing ?: MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, MusicService::class.java)),
        ).buildAsync().also { controllerFuture = it }

        suspendCancellableCoroutine { cont ->
            future.addListener(
                {
                    val controller = runCatching { future.get() }.getOrNull()
                    if (controller != null) {
                        action(controller)
                    }
                    if (cont.isActive) cont.resume(Unit) {}
                },
                ContextCompat.getMainExecutor(appContext),
            )
        }
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