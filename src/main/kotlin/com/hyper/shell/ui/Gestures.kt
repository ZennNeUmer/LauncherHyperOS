package com.hyper.shell.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hyper.shell.core.LauncherSettings
import kotlin.math.abs
import kotlin.math.max

enum class Overlay {
    DRAWER,
    SHADE
}

/**
 * Root-level gesture recognizer. It watches the Initial pointer pass, so it can
 * open panels even when the gesture starts on top of a widget, but it only
 * consumes events after the movement is unambiguously a launcher gesture. Taps,
 * long presses and widget interactions keep working.
 */
@Composable
fun Modifier.launcherGestures(
    enabled: Boolean,
    settings: LauncherSettings,
    onStart: (Overlay) -> Unit,
    onDrag: (Overlay, Float) -> Unit,
    onEnd: (Overlay, Float, Float) -> Unit
): Modifier {
    // Keep the detector node alive across recompositions: opening a panel changes
    // `enabled` and `settings`, and restarting the pointer loop would abort the
    // very gesture that opened it.
    val enabledState = rememberUpdatedState(enabled)
    val settingsState = rememberUpdatedState(settings)
    return pointerInput(Unit) {
        awaitEachGesture {
            if (!enabledState.value) return@awaitEachGesture
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val slop = viewConfiguration.touchSlop
            val edge = 52.dp.toPx()
            var chosen: Overlay? = null
            var total = Offset.Zero

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                total = change.position - down.position
                tracker.addPosition(change.uptimeMillis, change.position)

                if (chosen == null && total.getDistance() > slop) {
                    val candidate = chooseOverlay(
                        down.position,
                        total,
                        size,
                        settingsState.value,
                        edge
                    )
                    if (candidate != null) {
                        chosen = candidate
                        onStart(candidate)
                    }
                }

                val active = chosen
                if (active != null) {
                    change.consume()
                    onDrag(active, progressFor(active, total, size))
                }

                if (!change.pressed) break
            }

            val active = chosen
            if (active != null) {
                val velocity = tracker.calculateVelocity()
                val openVelocity = when (active) {
                    Overlay.DRAWER -> max(-velocity.x, -velocity.y)
                    Overlay.SHADE -> max(-velocity.x, velocity.y)
                }
                onEnd(active, progressFor(active, total, size), openVelocity)
            }
        }
    }
}

private fun chooseOverlay(
    start: Offset,
    total: Offset,
    size: IntSize,
    settings: LauncherSettings,
    edge: Float
): Overlay? {
    val dx = total.x
    val dy = total.y
    val adx = abs(dx)
    val ady = abs(dy)
    val fromRightEdge = start.x > size.width - edge
    val fromTop = start.y < size.height * 0.38f
    val fromLowerArea = start.y > size.height * 0.16f

    if (settings.edgeDrawer && fromRightEdge && dx < 0f && adx > ady * 0.75f) {
        return Overlay.DRAWER
    }
    if (settings.swipeUpDrawer && dy < 0f && ady > adx * 1.15f && fromLowerArea) {
        return Overlay.DRAWER
    }
    if (settings.swipeLeftShade && dx < 0f && adx > ady * 1.15f) {
        return Overlay.SHADE
    }
    if (settings.swipeDownShade && dy > 0f && ady > adx * 1.15f && fromTop) {
        return Overlay.SHADE
    }
    return null
}

private fun progressFor(overlay: Overlay, total: Offset, size: IntSize): Float {
    val distance = when (overlay) {
        Overlay.DRAWER -> max(-total.x, -total.y)
        Overlay.SHADE -> max(-total.x, total.y)
    }
    val span = size.width.coerceAtMost(size.height) * 0.55f
    return (distance / span).coerceIn(0f, 1f)
}
