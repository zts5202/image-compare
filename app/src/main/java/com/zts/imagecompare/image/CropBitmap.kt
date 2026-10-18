package com.zts.imagecompare.image

import android.graphics.Bitmap
import com.zts.imagecompare.core.CropMath
import com.zts.imagecompare.core.NormRect
import java.io.File

object CropBitmap {
    const val MAX_EDGE = 4096

    /**
     * Writes a new file cropped to [norm] of the oriented source. The source file is left in place.
     * [norm] uses the same top-left fractions reported by the cropper for the oriented image.
     */
    fun write(source: File, norm: NormRect, dest: File) {
        val decoded = ImageDecode.decode(source, MAX_EDGE) ?: error("无法解码图片")
        var cropped: Bitmap? = null
        try {
            val pixels = CropMath.toPixels(norm, decoded.width, decoded.height)
            cropped = Bitmap.createBitmap(decoded, pixels.left, pixels.top, pixels.width, pixels.height)
            val format = if (dest.extension.equals("png", ignoreCase = true)) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            dest.parentFile?.mkdirs()
            dest.outputStream().use { output ->
                if (!cropped.compress(format, 92, output)) error("无法保存裁剪")
            }
        } finally {
            if (cropped != null && cropped !== decoded && !cropped.isRecycled) cropped.recycle()
            if (!decoded.isRecycled) decoded.recycle()
        }
    }
}
