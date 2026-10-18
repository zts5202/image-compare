package com.zts.imagecompare.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CropMathTest {
    @Test
    fun normalizeRoundTripsThroughPixels() {
        val norm = CropMath.normalize(100, 40, 300, 200, 400, 250)
        val pixels = CropMath.toPixels(norm, 400, 250)
        assertEquals(100, pixels.left)
        assertEquals(40, pixels.top)
        assertEquals(200, pixels.width)
        assertEquals(160, pixels.height)
    }

    @Test
    fun squareFitOnWideImageIsCentered() {
        val fitted = CropMath.fitRatio(NormRect.Full, 200, 100, 1, 1)
        val pixels = CropMath.toPixels(fitted, 200, 100)
        assertEquals(100, pixels.width)
        assertEquals(100, pixels.height)
        assertEquals(50, pixels.left)
        assertEquals(0, pixels.top)
        assertEquals(1f, CropMath.pixelAspect(fitted, 200, 100), 0.02f)
    }

    @Test
    fun sourcePresetUsesOriginalRatioWhenUnified() {
        val preset = AspectPreset.SOURCE
        assertEquals(4 to 3, preset.ratio(9, 16, 400, 300, unify = true))
        assertEquals(9 to 16, preset.ratio(90, 160, 400, 300, unify = false))
        assertNull(AspectPreset.FREE.ratio(9, 16, 4, 3, unify = true))
        assertEquals(AspectPreset.THREE_FOUR, AspectPreset.fromKey("3:4"))
    }

    @Test
    fun syncCopiesTheNormalizedRectAndPreset() {
        val session = session()
        session.setSync(true)
        session.onWindow(NormRect(0.1f, 0.2f, 0.6f, 0.8f))
        val snap = session.snapshot()
        val original = snap.targets[0]
        val generated = snap.targets[1]
        assertTrue(CropMath.sameRect(original.norm, generated.norm))
        assertEquals(generated.preset, original.preset)
        val applied = session.decisions().filterIsInstance<CropDecision.Apply>()
        assertEquals(setOf("original", "gen"), applied.map { it.key }.toSet())
    }

    @Test
    fun unifyForcesBothImagesOntoTheSamePixelAspect() {
        val session = session()
        session.setPreset(AspectPreset.SQUARE)
        val snap = session.snapshot()
        val original = snap.targets[0]
        val generated = snap.targets[1]
        assertEquals(AspectPreset.SQUARE, original.preset)
        assertEquals(AspectPreset.SQUARE, generated.preset)
        assertEquals(1f, CropMath.pixelAspect(original.norm, original.imageW, original.imageH), 0.03f)
        assertEquals(1f, CropMath.pixelAspect(generated.norm, generated.imageW, generated.imageH), 0.03f)
        assertFalse(CropMath.sameRect(original.norm, generated.norm))
    }

    @Test
    fun restoreClearsTheActiveCrop() {
        val saved = CropSpec(NormRect(0.1f, 0.1f, 0.8f, 0.9f), AspectPreset.THREE_FOUR)
        val session = CropSession(
            inputs = listOf(
                CropInput("original", "原图", 400, 300, saved),
                CropInput("gen", "生成图", 300, 500),
            ),
            startIndex = 1,
        )
        session.setSync(false)
        session.restore()
        val decisions = session.decisions().associateBy { it.key }
        assertTrue(decisions.getValue("gen") is CropDecision.Clear)
        assertTrue(session.snapshot().targets[1].norm.coversImage())
    }

    @Test
    fun openingSuggestsOriginalAspectForTheGeneratedImageOnly() {
        val session = session()
        val decisions = session.decisions().associateBy { it.key }
        assertTrue(decisions.getValue("original") is CropDecision.Unchanged)
        val generated = decisions.getValue("gen") as CropDecision.Apply
        assertEquals(AspectPreset.SOURCE, generated.spec.preset)
        assertEquals(4f / 3f, CropMath.pixelAspect(generated.spec.norm, 300, 500), 0.03f)
        assertTrue(abs(CropMath.pixelAspect(generated.spec.norm, 300, 500) - 4f / 3f) < 0.03f)
    }

    private fun session(): CropSession = CropSession(
        inputs = listOf(
            CropInput("original", "原图", 400, 300),
            CropInput("gen", "生成图", 300, 500),
        ),
        startIndex = 1,
    )
}
