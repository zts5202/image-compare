package com.zts.imagecompare.image

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.zts.imagecompare.core.FaceBox
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** On-device face detector using the bundled ML Kit model. */
class FaceAligner {
    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.12f)
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun largestFace(bitmap: Bitmap): FaceBox? = suspendCancellableCoroutine { cont ->
        val task = try {
            detector.process(InputImage.fromBitmap(bitmap, 0))
        } catch (error: Throwable) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }
        task.addOnSuccessListener { faces ->
            val box = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }?.boundingBox
            if (cont.isActive) {
                cont.resume(
                    box?.let {
                        FaceBox(
                            left = it.left.toFloat(),
                            top = it.top.toFloat(),
                            right = it.right.toFloat(),
                            bottom = it.bottom.toFloat(),
                        )
                    },
                )
            }
        }
        task.addOnFailureListener {
            if (cont.isActive) cont.resume(null)
        }
    }
}
