package com.zts.imagecompare.core

import kotlin.math.abs

/**
 * Per-pixel difference mask. Values are alpha 0..255.
 * Pixels under [threshold] (mean absolute RGB delta) stay clear; the rest are
 * blurred with a clamped box filter so the highlight reads as regions, not noise.
 */
object DiffMask {
    fun differenceMask(
        left: IntArray,
        right: IntArray,
        width: Int,
        height: Int,
        threshold: Int = 30,
        blurRadius: Int = 2,
    ): IntArray {
        require(width > 0 && height > 0)
        require(left.size >= width * height && right.size >= width * height)
        val raw = IntArray(width * height)
        val denom = (255 - threshold).coerceAtLeast(1)
        for (i in raw.indices) {
            val delta = channelDelta(left[i], right[i])
            raw[i] = if (delta >= threshold) {
                (170 + (delta - threshold) * 85 / denom).coerceIn(0, 255)
            } else {
                0
            }
        }
        return boxBlur(raw, width, height, blurRadius)
    }

    fun channelDelta(a: Int, b: Int): Int {
        val dr = abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF))
        val dg = abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF))
        val db = abs((a and 0xFF) - (b and 0xFF))
        return (dr + dg + db) / 3
    }

    fun boxBlur(src: IntArray, width: Int, height: Int, radius: Int): IntArray {
        if (radius <= 0) return src.copyOf()
        val tmp = IntArray(src.size)
        val dst = IntArray(src.size)
        val window = radius * 2 + 1
        for (y in 0 until height) {
            val row = y * width
            var sum = 0
            for (k in -radius..radius) {
                sum += src[row + k.coerceIn(0, width - 1)]
            }
            tmp[row] = sum / window
            for (x in 1 until width) {
                val removeX = (x - radius - 1).coerceIn(0, width - 1)
                val addX = (x + radius).coerceIn(0, width - 1)
                sum += src[row + addX] - src[row + removeX]
                tmp[row + x] = sum / window
            }
        }
        for (x in 0 until width) {
            var sum = 0
            for (k in -radius..radius) {
                val y = k.coerceIn(0, height - 1)
                sum += tmp[y * width + x]
            }
            dst[x] = sum / window
            for (y in 1 until height) {
                val removeY = (y - radius - 1).coerceIn(0, height - 1)
                val addY = (y + radius).coerceIn(0, height - 1)
                sum += tmp[addY * width + x] - tmp[removeY * width + x]
                dst[y * width + x] = sum / window
            }
        }
        return dst
    }
}
