package com.zts.imagecompare.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.zts.imagecompare.image.ImageDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ComparisonRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)
    private val dao = database.comparisonDao()
    private val imagesRoot = File(appContext.filesDir, "images").apply { mkdirs() }
    private val stagingRoot = File(appContext.cacheDir, "staging").apply { mkdirs() }

    fun observeAll(): Flow<List<ComparisonWithImages>> = dao.observeAll()

    fun observe(id: Long): Flow<ComparisonWithImages?> = dao.observe(id)

    suspend fun stage(uri: Uri): File = withContext(Dispatchers.IO) {
        val mime = appContext.contentResolver.getType(uri)
        val ext = extensionFor(mime)
        val dest = File(stagingRoot, "${UUID.randomUUID()}.$ext")
        appContext.contentResolver.openInputStream(uri).use { input ->
            if (input == null) error("无法读取图片")
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        val bounds = ImageDecode.orientedBounds(dest)
        val probe = ImageDecode.decode(dest, 64)
        if (bounds.first <= 0 || bounds.second <= 0 || probe == null) {
            probe?.recycle()
            dest.delete()
            error("无法解码此图片")
        }
        probe.recycle()
        dest
    }

    fun clearStaging() {
        stagingRoot.listFiles()?.forEach { it.delete() }
    }

    fun discardStaged(files: List<File>) {
        files.forEach { file ->
            if (file.absolutePath.startsWith(stagingRoot.absolutePath)) file.delete()
        }
    }

    suspend fun create(original: File, generated: List<File>, category: String): Long = withContext(Dispatchers.IO) {
        require(generated.isNotEmpty())
        val dir = File(imagesRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        try {
            val originalDest = File(dir, "original.${original.extension.ifBlank { "jpg" }}")
            original.copyTo(originalDest, overwrite = true)
            val originalInfo = ImageDecode.readInfo(originalDest)
            if (originalInfo.width <= 0) error("无法解码原图")
            val generatedDests = generated.mapIndexed { index, file ->
                val dest = File(dir, "gen_$index.${file.extension.ifBlank { "jpg" }}")
                file.copyTo(dest, overwrite = true)
                dest
            }
            val generatedInfos = generatedDests.map { dest ->
                val info = ImageDecode.readInfo(dest)
                if (info.width <= 0) error("无法解码生成图")
                info
            }
            val comparisonId = database.withTransaction {
                val id = dao.insertComparison(
                    ComparisonEntity(
                        category = category,
                        originalPath = originalDest.absolutePath,
                        originalWidth = originalInfo.width,
                        originalHeight = originalInfo.height,
                        originalBytes = originalInfo.bytes,
                        originalSharpness = originalInfo.sharpness,
                    ),
                )
                val ids = dao.insertGenerated(
                    generatedInfos.mapIndexed { index, info ->
                        GeneratedImageEntity(
                            comparisonId = id,
                            path = generatedDests[index].absolutePath,
                            width = info.width,
                            height = info.height,
                            bytes = info.bytes,
                            sharpness = info.sharpness,
                            sortOrder = index,
                        )
                    },
                )
                val bestIndex = generatedInfos.indices.maxBy { generatedInfos[it].sharpness }
                dao.setBest(id, ids[bestIndex])
                id
            }
            discardStaged(listOf(original) + generated)
            comparisonId
        } catch (error: Throwable) {
            dir.deleteRecursively()
            throw error
        }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        dao.setFavorite(id, favorite)
    }

    suspend fun setBest(comparisonId: Long, generatedId: Long) {
        dao.setBest(comparisonId, generatedId)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val item = dao.get(id) ?: return@withContext
        dao.delete(id)
        File(item.comparison.originalPath).parentFile?.deleteRecursively()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
        imagesRoot.deleteRecursively()
        stagingRoot.deleteRecursively()
        imagesRoot.mkdirs()
        stagingRoot.mkdirs()
    }

    private fun extensionFor(mime: String?): String = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }
}
