package com.zts.imagecompare.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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
