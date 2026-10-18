package com.zts.imagecompare.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffMaskTest {
    @Test
    fun identicalImagesAreClear() {
        val pixels = IntArray(12) { gray(80) }
        val mask = DiffMask.differenceMask(pixels, pixels, 4, 3, threshold = 30, blurRadius = 1)
        assertTrue(mask.all { it == 0 })
    }

    @Test
    fun belowThresholdStaysClear() {
        val left = IntArray(9) { gray(0) }
        val right = IntArray(9) { gray(10) }
        val mask = DiffMask.differenceMask(left, right, 3, 3, threshold = 30, blurRadius = 1)
        assertTrue(mask.all { it == 0 })
    }

    @Test
    fun strongDifferenceFillsTheMask() {
        val left = IntArray(4) { gray(0) }
        val right = IntArray(4) { gray(255) }
        val mask = DiffMask.differenceMask(left, right, 2, 2, threshold = 30, blurRadius = 0)
        assertTrue(mask.all { it == 255 })
    }

    @Test
    fun blurSpreadsAnIsolatedDifferenceAndLeavesTheCornerClear() {
        val size = 5
        val left = IntArray(size * size) { gray(0) }
        val right = left.copyOf()
        right[2 * size + 2] = gray(255)
        val mask = DiffMask.differenceMask(left, right, size, size, threshold = 30, blurRadius = 1)
        val center = mask[2 * size + 2]
        val neighbor = mask[2 * size + 1]
        val corner = mask[0]
        assertTrue(center > 0)
        assertTrue(neighbor > 0)
        assertEquals(0, corner)
    }

    @Test
    fun blurKeepsAUniformField() {
        val src = IntArray(16) { 40 }
        val blurred = DiffMask.boxBlur(src, 4, 4, radius = 2)
        assertTrue(blurred.all { it == 40 })
    }

    @Test
    fun channelDeltaAveragesAbsoluteRgb() {
        val a = (0xFF shl 24) or (10 shl 16) or (20 shl 8) or 30
        val b = (0xFF shl 24) or (40 shl 16) or (20 shl 8) or 0
        assertEquals((30 + 0 + 30) / 3, DiffMask.channelDelta(a, b))
    }

    private fun gray(value: Int): Int {
        return (0xFF shl 24) or (value shl 16) or (value shl 8) or value
    }
}
