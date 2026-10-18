package com.zts.imagecompare.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.zts.imagecompare.core.AspectPreset
import com.zts.imagecompare.core.CropSpec
import com.zts.imagecompare.core.NormRect

enum class Category(val label: String) {
    HAIR("发型"),
    OUTFIT("换衣"),
    CLARITY("清晰度"),
    ;

    companion object {
        val labels = entries.map { it.label }
        fun fromLabel(label: String): Category? = entries.firstOrNull { it.label == label }
    }
}

@Entity(tableName = "comparisons")
data class ComparisonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val originalPath: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val originalBytes: Long,
    val originalSharpness: Int,
    val bestGeneratedId: Long? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val originalCropPath: String? = null,
    val originalCropLeft: Float? = null,
    val originalCropTop: Float? = null,
    val originalCropRight: Float? = null,
    val originalCropBottom: Float? = null,
    val originalCropAspect: String? = null,
    val originalCropWidth: Int? = null,
    val originalCropHeight: Int? = null,
    val originalCropBytes: Long? = null,
    val originalCropSharpness: Int? = null,
)

@Entity(
    tableName = "generated_images",
    foreignKeys = [
        ForeignKey(
            entity = ComparisonEntity::class,
            parentColumns = ["id"],
            childColumns = ["comparisonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comparisonId")],
)
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comparisonId: Long,
    val path: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val sharpness: Int,
    val sortOrder: Int,
    val cropPath: String? = null,
    val cropLeft: Float? = null,
    val cropTop: Float? = null,
    val cropRight: Float? = null,
    val cropBottom: Float? = null,
    val cropAspect: String? = null,
    val cropWidth: Int? = null,
    val cropHeight: Int? = null,
    val cropBytes: Long? = null,
    val cropSharpness: Int? = null,
)

data class ComparisonWithImages(
    @Embedded val comparison: ComparisonEntity,
    @Relation(parentColumn = "id", entityColumn = "comparisonId")
    val images: List<GeneratedImageEntity>,
) {
    val orderedImages: List<GeneratedImageEntity> get() = images.sortedBy { it.sortOrder }

    fun bestOrFirst(): GeneratedImageEntity? {
        val ordered = orderedImages
        return ordered.firstOrNull { it.id == comparison.bestGeneratedId } ?: ordered.firstOrNull()
    }
}

fun ComparisonEntity.cropSpec(): CropSpec? = cropSpecOf(
    originalCropPath,
    originalCropLeft,
    originalCropTop,
    originalCropRight,
    originalCropBottom,
    originalCropAspect,
)

fun ComparisonEntity.activeOriginalPath(): String = originalCropPath ?: originalPath

fun ComparisonEntity.activeOriginalWidth(): Int = if (originalCropPath != null) originalCropWidth ?: originalWidth else originalWidth

fun ComparisonEntity.activeOriginalHeight(): Int = if (originalCropPath != null) originalCropHeight ?: originalHeight else originalHeight

fun ComparisonEntity.activeOriginalBytes(): Long = if (originalCropPath != null) originalCropBytes ?: originalBytes else originalBytes

fun ComparisonEntity.activeOriginalSharpness(): Int =
    if (originalCropPath != null) originalCropSharpness ?: originalSharpness else originalSharpness

fun GeneratedImageEntity.cropSpec(): CropSpec? = cropSpecOf(
    cropPath,
    cropLeft,
    cropTop,
    cropRight,
    cropBottom,
    cropAspect,
)

fun GeneratedImageEntity.activePath(): String = cropPath ?: path

fun GeneratedImageEntity.activeWidth(): Int = if (cropPath != null) cropWidth ?: width else width

fun GeneratedImageEntity.activeHeight(): Int = if (cropPath != null) cropHeight ?: height else height

fun GeneratedImageEntity.activeBytes(): Long = if (cropPath != null) cropBytes ?: bytes else bytes

fun GeneratedImageEntity.activeSharpness(): Int = if (cropPath != null) cropSharpness ?: sharpness else sharpness

private fun cropSpecOf(
    path: String?,
    left: Float?,
    top: Float?,
    right: Float?,
    bottom: Float?,
    aspect: String?,
): CropSpec? {
    if (path.isNullOrBlank() || left == null || top == null || right == null || bottom == null) return null
    if (left >= right || top >= bottom) return null
    return CropSpec(NormRect(left, top, right, bottom), AspectPreset.fromKey(aspect))
}
