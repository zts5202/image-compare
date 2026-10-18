package com.zts.imagecompare.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.zts.imagecompare.core.CropDecision
import com.zts.imagecompare.core.CropSpec
import com.zts.imagecompare.image.CropBitmap
import com.zts.imagecompare.image.ImageDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ImportImage(
    val file: File,
    val cropFile: File? = null,
    val crop: CropSpec? = null,
)

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

    suspend fun create(original: ImportImage, generated: List<ImportImage>, category: String): Long = withContext(Dispatchers.IO) {
        require(generated.isNotEmpty())
        val dir = File(imagesRoot, UUID.randomUUID().toString()).apply { mkdirs() }
        try {
            val originalDest = File(dir, "original.${original.file.extension.ifBlank { "jpg" }}")
            original.file.copyTo(originalDest, overwrite = true)
            val originalInfo = ImageDecode.readInfo(originalDest)
            if (originalInfo.width <= 0) error("无法解码原图")
            val originalCrop = copyCrop(dir, "original_crop", original.cropFile, original.crop)
            val generatedDests = generated.mapIndexed { index, image ->
                val dest = File(dir, "gen_$index.${image.file.extension.ifBlank { "jpg" }}")
                image.file.copyTo(dest, overwrite = true)
                dest
            }
            val generatedInfos = generatedDests.map { dest ->
                val info = ImageDecode.readInfo(dest)
                if (info.width <= 0) error("无法解码生成图")
                info
            }
            val generatedCrops = generated.mapIndexed { index, image ->
                copyCrop(dir, "gen_${index}_crop", image.cropFile, image.crop)
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
                        originalCropPath = originalCrop?.path,
                        originalCropLeft = originalCrop?.left,
                        originalCropTop = originalCrop?.top,
                        originalCropRight = originalCrop?.right,
                        originalCropBottom = originalCrop?.bottom,
                        originalCropAspect = originalCrop?.aspect,
                        originalCropWidth = originalCrop?.width,
                        originalCropHeight = originalCrop?.height,
                        originalCropBytes = originalCrop?.bytes,
                        originalCropSharpness = originalCrop?.sharpness,
                    ),
                )
                val ids = dao.insertGenerated(
                    generatedInfos.mapIndexed { index, info ->
                        val crop = generatedCrops[index]
                        GeneratedImageEntity(
                            comparisonId = id,
                            path = generatedDests[index].absolutePath,
                            width = info.width,
                            height = info.height,
                            bytes = info.bytes,
                            sharpness = info.sharpness,
                            sortOrder = index,
                            cropPath = crop?.path,
                            cropLeft = crop?.left,
                            cropTop = crop?.top,
                            cropRight = crop?.right,
                            cropBottom = crop?.bottom,
                            cropAspect = crop?.aspect,
                            cropWidth = crop?.width,
                            cropHeight = crop?.height,
                            cropBytes = crop?.bytes,
                            cropSharpness = crop?.sharpness,
                        )
                    },
                )
                val scores = generatedInfos.mapIndexed { index, info ->
                    generatedCrops[index]?.sharpness ?: info.sharpness
                }
                val bestIndex = scores.indices.maxBy { scores[it] }
                dao.setBest(id, ids[bestIndex])
                id
            }
            discardStaged(listOf(original.file) + generated.map { it.file } + listOfNotNull(original.cropFile) + generated.mapNotNull { it.cropFile })
            comparisonId
        } catch (error: Throwable) {
            dir.deleteRecursively()
            throw error
        }
    }

    suspend fun stageCrop(source: File, spec: CropSpec): Pair<File, ImageDecode.Info> = withContext(Dispatchers.IO) {
        val ext = if (source.extension.equals("png", ignoreCase = true)) "png" else "jpg"
        val dest = File(stagingRoot, "crop-${UUID.randomUUID()}.$ext")
        CropBitmap.write(source, spec.norm, dest)
        val info = ImageDecode.readInfo(dest)
        if (info.width <= 0) {
            dest.delete()
            error("无法解码裁剪结果")
        }
        dest to info
    }

    suspend fun commitCrops(comparisonId: Long, decisions: List<CropDecision>) = withContext(Dispatchers.IO) {
        val item = dao.get(comparisonId) ?: error("对比不存在")
        val dir = File(item.comparison.originalPath).parentFile ?: error("无法保存裁剪")
        val written = mutableListOf<File>()
        try {
            data class PendingApply(
                val key: String,
                val spec: CropSpec,
                val file: File,
                val info: ImageDecode.Info,
            )
            val applies = mutableListOf<PendingApply>()
            val clears = mutableListOf<String>()
            for (decision in decisions) {
                when (decision) {
                    is CropDecision.Unchanged -> Unit
                    is CropDecision.Clear -> clears += decision.key
                    is CropDecision.Apply -> {
                        val source = sourceFile(item, decision.key)
                        val ext = if (source.extension.equals("png", ignoreCase = true)) "png" else "jpg"
                        val dest = File(dir, cropFileName(decision.key, ext))
                        CropBitmap.write(source, decision.spec.norm, dest)
                        written += dest
                        val info = ImageDecode.readInfo(dest)
                        if (info.width <= 0) error("无法解码裁剪结果")
                        applies += PendingApply(decision.key, decision.spec, dest, info)
                    }
                }
            }
            val previousCrops = mutableListOf<String>()
            database.withTransaction {
                for (apply in applies) {
                    previousCrops += replaceCrop(item, apply.key, apply.file, apply.info, apply.spec)
                }
                for (key in clears) {
                    previousCrops += clearCrop(item, key)
                }
            }
            previousCrops.filter { it.isNotBlank() }.forEach { path ->
                if (written.none { it.absolutePath == path }) File(path).delete()
            }
        } catch (error: Throwable) {
            written.forEach { it.delete() }
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

    private fun copyCrop(dir: File, name: String, cropFile: File?, spec: CropSpec?): StoredCrop? {
        if (cropFile == null || spec == null || !cropFile.exists()) return null
        val dest = File(dir, "$name.${cropFile.extension.ifBlank { "jpg" }}")
        cropFile.copyTo(dest, overwrite = true)
        val info = ImageDecode.readInfo(dest)
        if (info.width <= 0) error("无法解码裁剪结果")
        return StoredCrop(
            path = dest.absolutePath,
            left = spec.norm.left,
            top = spec.norm.top,
            right = spec.norm.right,
            bottom = spec.norm.bottom,
            aspect = spec.preset.key,
            width = info.width,
            height = info.height,
            bytes = info.bytes,
            sharpness = info.sharpness,
        )
    }

    private suspend fun replaceCrop(
        item: ComparisonWithImages,
        key: String,
        file: File,
        info: ImageDecode.Info,
        spec: CropSpec,
    ): String {
        val previous = currentCropPath(item, key)
        if (key == ORIGINAL_KEY) {
            dao.updateOriginalCrop(
                id = item.comparison.id,
                path = file.absolutePath,
                left = spec.norm.left,
                top = spec.norm.top,
                right = spec.norm.right,
                bottom = spec.norm.bottom,
                aspect = spec.preset.key,
                width = info.width,
                height = info.height,
                bytes = info.bytes,
                sharpness = info.sharpness,
            )
        } else {
            dao.updateGeneratedCrop(
                id = key.toLong(),
                path = file.absolutePath,
                left = spec.norm.left,
                top = spec.norm.top,
                right = spec.norm.right,
                bottom = spec.norm.bottom,
                aspect = spec.preset.key,
                width = info.width,
                height = info.height,
                bytes = info.bytes,
                sharpness = info.sharpness,
            )
        }
        return previous.orEmpty()
    }

    private suspend fun clearCrop(item: ComparisonWithImages, key: String): String {
        val previous = currentCropPath(item, key)
        if (key == ORIGINAL_KEY) {
            dao.clearOriginalCrop(item.comparison.id)
        } else {
            dao.clearGeneratedCrop(key.toLong())
        }
        return previous.orEmpty()
    }

    private fun sourceFile(item: ComparisonWithImages, key: String): File {
        val path = if (key == ORIGINAL_KEY) {
            item.comparison.originalPath
        } else {
            item.images.firstOrNull { it.id == key.toLong() }?.path ?: error("找不到要裁剪的图片")
        }
        return File(path)
    }

    private fun currentCropPath(item: ComparisonWithImages, key: String): String? {
        return if (key == ORIGINAL_KEY) {
            item.comparison.originalCropPath
        } else {
            item.images.firstOrNull { it.id == key.toLong() }?.cropPath
        }
    }

    private fun cropFileName(key: String, extension: String): String {
        val prefix = if (key == ORIGINAL_KEY) "original_crop" else "gen_${key}_crop"
        return "${prefix}_${UUID.randomUUID()}.$extension"
    }

    private data class StoredCrop(
        val path: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val aspect: String,
        val width: Int,
        val height: Int,
        val bytes: Long,
        val sharpness: Int,
    )

    companion object {
        const val ORIGINAL_KEY = "original"
    }
}
