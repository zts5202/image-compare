package com.zts.imagecompare.ui.compare

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal suspend fun PointerInputScope.compareGestures(
    modeOf: () -> CompareMode,
    splitOf: () -> Float,
    zoomedOf: () -> Boolean,
    onSplit: (Float) -> Unit,
    onZoom: (pan: Offset, zoom: Float, centroid: Offset) -> Unit,
    onTap: (Offset) -> Unit,
    onLoupeBegin: (Offset) -> Unit,
    onLoupeMove: (Offset) -> Unit,
    onLoupeEnd: () -> Unit,
    onHoldChanged: (Boolean) -> Unit,
) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val dividerSlop = 32.dp.toPx()

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        val pointerId = down.id
        val start = down.position
        var position = start
        val mode = modeOf()

        if (mode == CompareMode.Split && abs(start.x - splitOf() * size.width) <= dividerSlop) {
            down.consume()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                if (!change.pressed) break
                change.consume()
                onSplit((change.position.x / size.width).coerceIn(0.06f, 0.94f))
            }
            return@awaitEachGesture
        }

        if (mode == CompareMode.Hold) onHoldChanged(true)
        var moved = false
        var longPressed = false
        var transformed = false
        try {
            while (true) {
                val elapsed = android.os.SystemClock.uptimeMillis() - down.uptimeMillis
                val remaining = longPressTimeout - elapsed
                val event = if (!longPressed && !moved && !transformed && remaining > 0L) {
                    try {
                        withTimeout(remaining) { awaitPointerEvent() }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        null
                    }
                } else {
                    awaitPointerEvent()
                }
                if (event == null) {
                    longPressed = true
                    onLoupeBegin(position)
                    continue
                }
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    if (longPressed) {
                        onLoupeEnd()
                        longPressed = false
                    }
                    transformed = true
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid(useCurrent = true)
                    if (centroid != Offset.Unspecified) onZoom(pan, zoom, centroid)
                    event.changes.forEach { it.consume() }
                    continue
                }
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) {
                    if (!moved && !longPressed && !transformed) onTap(start)
                    break
                }
                position = change.position
                if (!moved && (position - start).getDistance() > touchSlop) moved = true
                if (longPressed) {
                    change.consume()
                    onLoupeMove(position)
                } else if (moved && zoomedOf() && mode != CompareMode.Hold) {
                    change.consume()
                    val delta = change.position - change.previousPosition
                    onZoom(delta, 1f, change.position)
                }
            }
        } finally {
            if (mode == CompareMode.Hold) onHoldChanged(false)
            if (longPressed) onLoupeEnd()
        }
    }
}
