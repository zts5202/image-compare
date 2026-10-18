package com.zts.imagecompare.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharpnessTest {
    @Test
    fun uniformImageScoresZero() {
        val pixels = IntArray(16) { gray(40) }
        assertEquals(0.0, Sharpness.laplacianVariance(pixels, 4, 4), 1e-6)
        assertEquals(0, Sharpness.score(pixels, 4, 4))
    }

    @Test
    fun knownLaplacianVariance() {
        val width = 4
        val height = 4
        val values = intArrayOf(
            0, 0, 0, 0,
            0, 10, 20, 0,
            0, 30, 40, 0,
            0, 0, 0, 0,
        )
        val pixels = IntArray(values.size) { gray(values[it]) }
        assertEquals(2000.0, Sharpness.laplacianVariance(pixels, width, height), 1e-6)
    }

    @Test
    fun checkerboardOutscoresFlatImage() {
        val sharp = checkerboard(32)
        val flat = IntArray(32 * 32) { gray(128) }
        val sharpScore = Sharpness.score(sharp, 32, 32)
        val flatScore = Sharpness.score(flat, 32, 32)
        assertEquals(100, sharpScore)
        assertEquals(0, flatScore)
        assertTrue(sharpScore > flatScore)
    }

    @Test
    fun scoreFromVarianceCheckpoints() {
        assertEquals(0, Sharpness.scoreFromVariance(0.0))
        assertEquals(0, Sharpness.scoreFromVariance(-5.0))
        assertEquals(54, Sharpness.scoreFromVariance(100.0))
        assertEquals(100, Sharpness.scoreFromVariance(5000.0))
        assertEquals(100, Sharpness.scoreFromVariance(1_000_000.0))
    }

    @Test
    fun tinyImageIsZero() {
        assertEquals(0.0, Sharpness.laplacianVariance(IntArray(4) { gray(10) }, 2, 2), 0.0)
    }

    private fun checkerboard(size: Int): IntArray {
        return IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            gray(if ((x + y) % 2 == 0) 0 else 255)
        }
    }

    private fun gray(value: Int): Int {
        return (0xFF shl 24) or (value shl 16) or (value shl 8) or value
    }
}
