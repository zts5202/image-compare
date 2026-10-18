package com.zts.imagecompare.core

/**
 * Crop rectangle in fractions of the oriented image, origin at the top-left.
 * [right] and [bottom] are exclusive edges, so the full image is (0, 0, 1, 1).
 */
data class NormRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun coversImage(epsilon: Float = 0.004f): Boolean {
        return left <= epsilon && top <= epsilon && right >= 1f - epsilon && bottom >= 1f - epsilon
    }

    companion object {
        val Full = NormRect(0f, 0f, 1f, 1f)
    }
}

data class PixelRect(val left: Int, val top: Int, val width: Int, val height: Int)

enum class AspectPreset(val key: String, val label: String) {
    SOURCE("source", "原图比例"),
    SQUARE("1:1", "1:1"),
    THREE_FOUR("3:4", "3:4"),
    FOUR_THREE("4:3", "4:3"),
    NINE_SIXTEEN("9:16", "9:16"),
    FREE("free", "自由"),
    ;

    /**
     * Pixel width/height to lock, or null when the crop is free.
     * [unify] makes [SOURCE] follow the original image instead of each file's own ratio.
     */
    fun ratio(imageW: Int, imageH: Int, sourceW: Int, sourceH: Int, unify: Boolean): Pair<Int, Int>? {
        return when (this) {
            FREE -> null
            SOURCE -> {
                val w = if (unify) sourceW else imageW
                val h = if (unify) sourceH else imageH
                if (w <= 0 || h <= 0) null else CropMath.reduced(w, h)
            }
            SQUARE -> 1 to 1
            THREE_FOUR -> 3 to 4
            FOUR_THREE -> 4 to 3
            NINE_SIXTEEN -> 9 to 16
        }
    }

    companion object {
        fun fromKey(key: String?): AspectPreset = entries.firstOrNull { it.key == key } ?: SOURCE
    }
}

data class CropSpec(val norm: NormRect, val preset: AspectPreset)

object CropMath {
    fun reduced(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 1 to 1
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val divisor = gcd(width, height)
        return width / divisor to height / divisor
    }

    fun normalize(left: Int, top: Int, right: Int, bottom: Int, imageW: Int, imageH: Int): NormRect {
        if (imageW <= 1 || imageH <= 1) return NormRect.Full
        return clamp(
            NormRect(
                left = left.toFloat() / imageW,
                top = top.toFloat() / imageH,
                right = right.toFloat() / imageW,
                bottom = bottom.toFloat() / imageH,
            ),
        )
    }

    fun clamp(rect: NormRect): NormRect {
        val left = rect.left.coerceIn(0f, 0.98f)
        val top = rect.top.coerceIn(0f, 0.98f)
        val right = rect.right.coerceIn(left + 0.02f, 1f)
        val bottom = rect.bottom.coerceIn(top + 0.02f, 1f)
        return NormRect(left, top, right, bottom)
    }

    fun toPixels(rect: NormRect, imageW: Int, imageH: Int): PixelRect {
        if (imageW <= 1 || imageH <= 1) return PixelRect(0, 0, imageW.coerceAtLeast(1), imageH.coerceAtLeast(1))
        val safe = clamp(rect)
        val left = (safe.left * imageW).toInt().coerceIn(0, imageW - 1)
        val top = (safe.top * imageH).toInt().coerceIn(0, imageH - 1)
        val right = (safe.right * imageW).toInt().coerceIn(left + 1, imageW)
        val bottom = (safe.bottom * imageH).toInt().coerceIn(top + 1, imageH)
        return PixelRect(left, top, right - left, bottom - top)
    }

    /**
     * Largest window of [ratioW]:[ratioH] (pixel width:height) that fits in the image,
     * centered on [anchor].
     */
    fun fitRatio(anchor: NormRect, imageW: Int, imageH: Int, ratioW: Int, ratioH: Int): NormRect {
        if (imageW <= 0 || imageH <= 0 || ratioW <= 0 || ratioH <= 0) return clamp(anchor)
        val target = ratioW.toFloat() / ratioH.toFloat()
        val imageAspect = imageW.toFloat() / imageH.toFloat()
        val normW: Float
        val normH: Float
        if (target >= imageAspect) {
            normW = 1f
            normH = (imageAspect / target).coerceIn(0.02f, 1f)
        } else {
            normH = 1f
            normW = (target / imageAspect).coerceIn(0.02f, 1f)
        }
        val centerX = ((anchor.left + anchor.right) / 2f).coerceIn(0f, 1f)
        val centerY = ((anchor.top + anchor.bottom) / 2f).coerceIn(0f, 1f)
        var left = centerX - normW / 2f
        var top = centerY - normH / 2f
        if (left < 0f) left = 0f
        if (top < 0f) top = 0f
        if (left + normW > 1f) left = 1f - normW
        if (top + normH > 1f) top = 1f - normH
        return NormRect(left, top, left + normW, top + normH)
    }

    fun pixelAspect(norm: NormRect, imageW: Int, imageH: Int): Float {
        val width = norm.width * imageW
        val height = norm.height * imageH
        if (height <= 0f) return 1f
        return width / height
    }

    fun sameRect(first: NormRect, second: NormRect, epsilon: Float = 0.004f): Boolean {
        return kotlin.math.abs(first.left - second.left) <= epsilon &&
            kotlin.math.abs(first.top - second.top) <= epsilon &&
            kotlin.math.abs(first.right - second.right) <= epsilon &&
            kotlin.math.abs(first.bottom - second.bottom) <= epsilon
    }
}
