package com.zts.imagecompare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ComparisonDao {
    @Transaction
    @Query("SELECT * FROM comparisons ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ComparisonWithImages>>

    @Transaction
    @Query("SELECT * FROM comparisons WHERE id = :id")
    fun observe(id: Long): Flow<ComparisonWithImages?>

    @Transaction
    @Query("SELECT * FROM comparisons WHERE id = :id")
    suspend fun get(id: Long): ComparisonWithImages?

    @Insert
    suspend fun insertComparison(entity: ComparisonEntity): Long

    @Insert
    suspend fun insertGenerated(images: List<GeneratedImageEntity>): List<Long>

    @Query("UPDATE comparisons SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE comparisons SET bestGeneratedId = :generatedId WHERE id = :id")
    suspend fun setBest(id: Long, generatedId: Long)

    @Query("DELETE FROM comparisons WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM comparisons")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE comparisons SET
            originalCropPath = :path,
            originalCropLeft = :left,
            originalCropTop = :top,
            originalCropRight = :right,
            originalCropBottom = :bottom,
            originalCropAspect = :aspect,
            originalCropWidth = :width,
            originalCropHeight = :height,
            originalCropBytes = :bytes,
            originalCropSharpness = :sharpness
        WHERE id = :id
        """,
    )
    suspend fun updateOriginalCrop(
        id: Long,
        path: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        aspect: String,
        width: Int,
        height: Int,
        bytes: Long,
        sharpness: Int,
    )

    @Query(
        """
        UPDATE comparisons SET
            originalCropPath = NULL,
            originalCropLeft = NULL,
            originalCropTop = NULL,
            originalCropRight = NULL,
            originalCropBottom = NULL,
            originalCropAspect = NULL,
            originalCropWidth = NULL,
            originalCropHeight = NULL,
            originalCropBytes = NULL,
            originalCropSharpness = NULL
        WHERE id = :id
        """,
    )
    suspend fun clearOriginalCrop(id: Long)

    @Query(
        """
        UPDATE generated_images SET
            cropPath = :path,
            cropLeft = :left,
            cropTop = :top,
            cropRight = :right,
            cropBottom = :bottom,
            cropAspect = :aspect,
            cropWidth = :width,
            cropHeight = :height,
            cropBytes = :bytes,
            cropSharpness = :sharpness
        WHERE id = :id
        """,
    )
    suspend fun updateGeneratedCrop(
        id: Long,
        path: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        aspect: String,
        width: Int,
        height: Int,
        bytes: Long,
        sharpness: Int,
    )

    @Query(
        """
        UPDATE generated_images SET
            cropPath = NULL,
            cropLeft = NULL,
            cropTop = NULL,
            cropRight = NULL,
            cropBottom = NULL,
            cropAspect = NULL,
            cropWidth = NULL,
            cropHeight = NULL,
            cropBytes = NULL,
            cropSharpness = NULL
        WHERE id = :id
        """,
    )
    suspend fun clearGeneratedCrop(id: Long)
}
