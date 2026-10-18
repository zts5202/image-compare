package com.zts.imagecompare.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AlignAndZoomTest {
    @Test
    fun missingFaceFallsBackToCenterFit() {
        val alignment = AlignMath.alignFaces(
            viewW = 100f,
            viewH = 100f,
            originalW = 100f,
            originalH = 100f,
            generatedW = 80f,
            generatedH = 80f,
            originalFace = FaceBox(40f, 40f, 60f, 60f),
            generatedFace = null,
        )
        assertEquals(Alignment.Identity, alignment)
    }

    @Test
    fun facesScaleAndTranslateOntoEachOther() {
        val alignment = AlignMath.alignFaces(
            viewW = 100f,
            viewH = 100f,
            originalW = 100f,
            originalH = 100f,
            generatedW = 100f,
            generatedH = 100f,
            originalFace = FaceBox(40f, 40f, 60f, 60f),
            generatedFace = FaceBox(65f, 35f, 75f, 45f),
        )
        assertEquals(2f, alignment.scale, 0.001f)
        assertEquals(0.7f, alignment.pivotXFraction, 0.001f)
        assertEquals(0.4f, alignment.pivotYFraction, 0.001f)
        assertEquals(-20f, alignment.translationX, 0.001f)
        assertEquals(10f, alignment.translationY, 0.001f)
    }

    @Test
    fun zoomKeepsTheCentroidContentStable() {
        val zoomed = ZoomMath.apply(
            current = Zoom(),
            panX = 0f,
            panY = 0f,
            zoom = 2f,
            centroidX = 50f,
            centroidY = 50f,
            viewW = 100f,
            viewH = 100f,
        )
        assertEquals(2f, zoomed.scale, 0.001f)
        assertEquals(-50f, zoomed.offsetX, 0.001f)
        assertEquals(-50f, zoomed.offsetY, 0.001f)
        val (x, y) = ZoomMath.contentPoint(zoomed, 50f, 50f)
        assertEquals(50f, x, 0.001f)
        assertEquals(50f, y, 0.001f)
    }

    @Test
    fun panAtUnitScaleIsClamped() {
        val zoomed = ZoomMath.apply(
            current = Zoom(),
            panX = 30f,
            panY = -20f,
            zoom = 1f,
            centroidX = 10f,
            centroidY = 10f,
            viewW = 100f,
            viewH = 80f,
        )
        assertEquals(1f, zoomed.scale, 0.001f)
        assertEquals(0f, zoomed.offsetX, 0.001f)
        assertEquals(0f, zoomed.offsetY, 0.001f)
    }
}
