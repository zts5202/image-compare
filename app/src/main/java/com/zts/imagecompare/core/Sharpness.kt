package com.zts.imagecompare.core

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Sharpness as the variance of a discrete Laplacian, mapped to 0..100.
 * A Laplacian variance of [REFERENCE_VARIANCE] scores 100; quieter images score less.
 */
object Sharpness {
    const val REFERENCE_VARIANCE = 5000.0

    fun score(pixels: IntArray, width: Int, height: Int): Int {
        return scoreFromVariance(laplacianVariance(pixels, width, height))
    }

    fun scoreFromVariance(variance: Double): Int {
        if (variance <= 0.0 || variance.isNaN()) return 0
        val normalized = ln(1.0 + variance) / ln(1.0 + REFERENCE_VARIANCE)
        return (normalized * 100.0).roundToInt().coerceIn(0, 100)
    }

    /**
     * Population variance of the 4-neighbour Laplacian over interior pixels.
     * Luminance is Rec. 601. Images smaller than 3×3 score 0.
     */
    fun laplacianVariance(pixels: IntArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3 || pixels.size < width * height) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val center = luminance(pixels[row + x])
                val lap = luminance(pixels[row - width + x]) +
                    luminance(pixels[row + width + x]) +
                    luminance(pixels[row + x - 1]) +
                    luminance(pixels[row + x + 1]) -
                    4.0 * center
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        val variance = sumSq / count - mean * mean
        return if (variance < 0.0) 0.0 else variance
    }

    private fun luminance(pixel: Int): Double {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000.0
    }
}
