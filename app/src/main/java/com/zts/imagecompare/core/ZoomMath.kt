package com.zts.imagecompare.core

/**
 * Pinch-zoom state. Scale is around the top-left of the view; [offsetX]/[offsetY] are
 * the graphicsLayer translation in view pixels. The content point under the fingers stays put.
 */
data class Zoom(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

object ZoomMath {
    fun apply(
        current: Zoom,
        panX: Float,
        panY: Float,
        zoom: Float,
        centroidX: Float,
        centroidY: Float,
        viewW: Float,
        viewH: Float,
        minScale: Float = 1f,
        maxScale: Float = 6f,
    ): Zoom {
        val safeZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
        val newScale = (current.scale * safeZoom).coerceIn(minScale, maxScale)
        val scaleChange = if (current.scale == 0f) 1f else newScale / current.scale
        var tx = (current.offsetX - centroidX) * scaleChange + centroidX + panX
        var ty = (current.offsetY - centroidY) * scaleChange + centroidY + panY
        val maxX = viewW * (newScale - 1f)
        val maxY = viewH * (newScale - 1f)
        tx = tx.coerceIn(-maxX, 0f)
        ty = ty.coerceIn(-maxY, 0f)
        return Zoom(newScale, tx, ty)
    }

    fun contentPoint(zoom: Zoom, viewX: Float, viewY: Float): Pair<Float, Float> {
        val s = if (zoom.scale == 0f) 1f else zoom.scale
        return (viewX - zoom.offsetX) / s to (viewY - zoom.offsetY) / s
    }
}
