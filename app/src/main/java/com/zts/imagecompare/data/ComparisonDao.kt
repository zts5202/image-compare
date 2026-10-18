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
}
