package com.zts.imagecompare.core

/**
 * Extra transform applied to the generated image so its face lands on the original face.
 * [scale] is around [pivotXFraction]/[pivotYFraction] of the view, then [translationX]/[translationY]
 * (view pixels) are added. Identity means each image is independently center-fitted.
 */
data class Alignment(
    val scale: Float = 1f,
    val pivotXFraction: Float = 0.5f,
    val pivotYFraction: Float = 0.5f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
) {
    val isIdentity: Boolean
        get() = scale == 1f && translationX == 0f && translationY == 0f

    companion object {
        val Identity = Alignment()
    }
}

data class FaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class FitRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
)

object AlignMath {
    fun fitCenter(srcW: Float, srcH: Float, dstW: Float, dstH: Float): FitRect {
        if (srcW <= 0f || srcH <= 0f || dstW <= 0f || dstH <= 0f) {
            return FitRect(0f, 0f, 0f, 0f, 1f)
        }
        val scale = minOf(dstW / srcW, dstH / srcH)
        val width = srcW * scale
        val height = srcH * scale
        return FitRect(
            left = (dstW - width) / 2f,
            top = (dstH - height) / 2f,
            width = width,
            height = height,
            scale = scale,
        )
    }

    /**
     * Scale and translate the generated image so the two face boxes coincide in the view.
     * Missing or empty faces fall back to independent center-fit.
     */
    fun alignFaces(
        viewW: Float,
        viewH: Float,
        originalW: Float,
        originalH: Float,
        generatedW: Float,
        generatedH: Float,
        originalFace: FaceBox?,
        generatedFace: FaceBox?,
    ): Alignment {
        if (originalFace == null || generatedFace == null) return Alignment.Identity
        if (originalFace.width <= 1f || originalFace.height <= 1f) return Alignment.Identity
        if (generatedFace.width <= 1f || generatedFace.height <= 1f) return Alignment.Identity
        if (viewW <= 0f || viewH <= 0f) return Alignment.Identity

        val origFit = fitCenter(originalW, originalH, viewW, viewH)
        val genFit = fitCenter(generatedW, generatedH, viewW, viewH)
        if (origFit.scale <= 0f || genFit.scale <= 0f) return Alignment.Identity

        val origFaceW = originalFace.width * origFit.scale
        val origFaceH = originalFace.height * origFit.scale
        val genFaceW = generatedFace.width * genFit.scale
        val genFaceH = generatedFace.height * genFit.scale
        if (genFaceW <= 1f || genFaceH <= 1f) return Alignment.Identity

        val extra = ((origFaceW / genFaceW) + (origFaceH / genFaceH)) / 2f
        if (!extra.isFinite() || extra <= 0f) return Alignment.Identity

        val genCenterX = genFit.left + generatedFace.centerX * genFit.scale
        val genCenterY = genFit.top + generatedFace.centerY * genFit.scale
        val origCenterX = origFit.left + originalFace.centerX * origFit.scale
        val origCenterY = origFit.top + originalFace.centerY * origFit.scale

        return Alignment(
            scale = extra,
            pivotXFraction = (genCenterX / viewW).coerceIn(0f, 1f),
            pivotYFraction = (genCenterY / viewH).coerceIn(0f, 1f),
            translationX = origCenterX - genCenterX,
            translationY = origCenterY - genCenterY,
        )
    }
}
