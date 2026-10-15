package com.m3u.smartphone.ui.material.components

import android.view.Surface
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.m3u.core.foundation.architecture.preferences.ClipMode
import kotlin.math.roundToInt

@Immutable
data class PlayerState(
    val player: Player?,
    @ClipMode val clipMode: Int,
    val keepScreenOn: Boolean,
    val zoom: Float = 1f
) {
    var surface: Surface? by mutableStateOf(null)
}

@Composable
fun rememberPlayerState(
    player: Player?,
    @ClipMode clipMode: Int = ClipMode.ADAPTIVE,
    keepScreenOn: Boolean = true,
    zoom: Float = 1f,
): PlayerState {
    return remember(
        player,
        clipMode,
        keepScreenOn,
        zoom
    ) {
        PlayerState(
            player,
            clipMode,
            keepScreenOn,
            zoom
        )
    }
}

private fun Modifier.videoZoom(zoom: Float): Modifier {
    if (zoom <= 1f) return this
    return this.then(Modifier.layout { measurable, constraints ->
        fun scale(v: Int) =
            if (v == Constraints.Infinity) Constraints.Infinity
            else (v * zoom).roundToInt().coerceAtLeast(1)
        val child = measurable.measure(
            constraints.copy(
                minWidth = scale(constraints.minWidth),
                maxWidth = scale(constraints.maxWidth),
                minHeight = scale(constraints.minHeight),
                maxHeight = scale(constraints.maxHeight)
            )
        )
        fun unscale(v: Int) =
            if (v == Constraints.Infinity) Constraints.Infinity
            else (v / zoom).roundToInt()
        val w = unscale(child.width)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val h = unscale(child.height)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(w, h) {
            child.place(-(child.width - w) / 2, -(child.height - h) / 2)
        }
    })
}

@OptIn(UnstableApi::class)
@Composable
fun Player(
    state: PlayerState,
    modifier: Modifier = Modifier
) {
    val player = state.player
    val keepScreenOn = state.keepScreenOn
    val clipMode = state.clipMode
    val zoom = state.zoom

    AndroidView(
        modifier = modifier.videoZoom(zoom),
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                videoSurfaceView
            }
        },
        update = { view ->
            view.apply {
                this.player = player
                setKeepScreenOn(keepScreenOn)
                resizeMode = when (clipMode) {
                    ClipMode.ADAPTIVE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ClipMode.CLIP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ClipMode.STRETCHED -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        },
        onRelease = {
            it.player = null
            it.keepScreenOn = false
        }
    )
}
