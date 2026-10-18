package com.zts.imagecompare.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.zts.imagecompare.core.AlignMath
import com.zts.imagecompare.core.Alignment
import com.zts.imagecompare.core.DiffMask
import kotlin.math.max
import kotlin.math.roundToInt

object DiffRenderer {
    private const val OUT_WIDTH = 280
    private const val TEAL = 0x00E5B0

    fun render(
        original: Bitmap,
        generated: Bitmap,
        alignment: Alignment,
        viewW: Int,
        viewH: Int,
    ): Bitmap? {
        if (viewW < 2 || viewH < 2) return null
        val outW = OUT_WIDTH
        val outH = max(2, (viewH.toFloat() * outW / viewW).roundToInt())
        val scale = outW.toFloat() / viewW
        val left = drawFitted(original, outW, outH, Alignment.Identity)
        val scaledAlign = Alignment(
            scale = alignment.scale,
            pivotXFraction = alignment.pivotXFraction,
            pivotYFraction = alignment.pivotYFraction,
            translationX = alignment.translationX * scale,
            translationY = alignment.translationY * scale,
        )
        val right = drawFitted(generated, outW, outH, scaledAlign)
        val mask = DiffMask.differenceMask(left, right, outW, outH)
        val pixels = IntArray(mask.size) { index ->
            val alpha = mask[index]
            if (alpha == 0) 0 else (alpha shl 24) or TEAL
        }
        return Bitmap.createBitmap(pixels, outW, outH, Bitmap.Config.ARGB_8888)
    }

    private fun drawFitted(bitmap: Bitmap, viewW: Int, viewH: Int, alignment: Alignment): IntArray {
        val output = Bitmap.createBitmap(viewW, viewH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.rgb(0x0E, 0x0F, 0x11))
        val fit = AlignMath.fitCenter(
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            viewW.toFloat(),
            viewH.toFloat(),
        )
        canvas.save()
        if (!alignment.isIdentity) {
            val pivotX = alignment.pivotXFraction * viewW
            val pivotY = alignment.pivotYFraction * viewH
            canvas.translate(alignment.translationX, alignment.translationY)
            canvas.translate(pivotX, pivotY)
            canvas.scale(alignment.scale, alignment.scale)
            canvas.translate(-pivotX, -pivotY)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(fit.left, fit.top, fit.left + fit.width, fit.top + fit.height),
            paint,
        )
        canvas.restore()
        val pixels = IntArray(viewW * viewH)
        output.getPixels(pixels, 0, viewW, 0, 0, viewW, viewH)
        output.recycle()
        return pixels
    }
}
