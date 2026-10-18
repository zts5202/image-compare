package com.zts.imagecompare.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.zts.imagecompare.core.Sharpness
import java.io.File
import kotlin.math.max

object ImageDecode {
    data class Info(
        val width: Int,
        val height: Int,
        val bytes: Long,
        val sharpness: Int,
    )

    fun readInfo(file: File): Info {
        val (width, height) = orientedBounds(file)
        val bitmap = decode(file, maxEdge = 512)
        val score = bitmap?.let { sharpnessOf(it) } ?: 0
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        return Info(
            width = width,
            height = height,
            bytes = file.length(),
            sharpness = score,
        )
    }

    fun sharpnessOf(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return Sharpness.score(pixels, bitmap.width, bitmap.height)
    }

    fun orientedBounds(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return 0 to 0
        val degrees = runCatching { ExifInterface(file.absolutePath).rotationDegrees }.getOrDefault(0)
        return if (degrees == 90 || degrees == 270) {
            options.outHeight to options.outWidth
        } else {
            options.outWidth to options.outHeight
        }
    }

    fun decode(file: File, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return applyExif(file, decoded)
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val longEdge = max(width, height)
        if (maxEdge <= 0) return 1
        while (longEdge / sample > maxEdge) {
            sample *= 2
        }
        return sample
    }

    private fun applyExif(file: File, bitmap: Bitmap): Bitmap {
        val degrees = runCatching { ExifInterface(file.absolutePath).rotationDegrees }.getOrDefault(0)
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
}
