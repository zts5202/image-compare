package com.zts.imagecompare.ui.compare

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zts.imagecompare.Graph
import com.zts.imagecompare.core.AlignMath
import com.zts.imagecompare.core.Alignment
import com.zts.imagecompare.core.FaceBox
import com.zts.imagecompare.data.ComparisonRepository
import com.zts.imagecompare.data.ComparisonWithImages
import com.zts.imagecompare.data.GeneratedImageEntity
import com.zts.imagecompare.image.DiffRenderer
import com.zts.imagecompare.image.FaceAligner
import com.zts.imagecompare.image.ImageDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GeneratedThumb(
    val id: Long,
    val path: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val sharpness: Int,
    val thumb: ImageBitmap? = null,
)

data class CompareUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val category: String = "",
    val favorite: Boolean = false,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalBytes: Long = 0,
    val originalSharpness: Int = 0,
    val generated: List<GeneratedThumb> = emptyList(),
    val selectedId: Long? = null,
    val bestId: Long? = null,
    val originalImage: ImageBitmap? = null,
    val selectedImage: ImageBitmap? = null,
    val autoAlign: Boolean = false,
    val alignBusy: Boolean = false,
    val alignment: Alignment = Alignment.Identity,
    val diffOn: Boolean = false,
    val diffBusy: Boolean = false,
    val diffImage: ImageBitmap? = null,
    val diffGeneration: Int = 0,
)

class CompareViewModel(
    private val repository: ComparisonRepository,
    private val comparisonId: Long,
) : ViewModel() {
    var ui by mutableStateOf(CompareUiState())
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private val aligner = FaceAligner()
    private var originalBitmap: Bitmap? = null
    private var selectedBitmap: Bitmap? = null
    private var originalFace: FaceBox? = null
    private var originalFaceLoaded = false
    private val generatedFaces = HashMap<Long, FaceBox?>()
    private val generatedFaceLoaded = HashSet<Long>()
    private var slotW = 0
    private var slotH = 0
    private var loadedKey: String? = null
    private var alignToken = 0
    private var diffToken = 0
    private var alignJob: Job? = null
    private var diffJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observe(comparisonId).collect { item ->
                if (item == null) {
                    ui = ui.copy(loading = false, missing = true)
                } else {
                    apply(item)
                }
            }
        }
    }

    fun consumeMessage() {
        message = null
    }

    fun onSlotSize(width: Int, height: Int) {
        if (width < 2 || height < 2) return
        if (width == slotW && height == slotH) return
        slotW = width
        slotH = height
        if (ui.autoAlign) refreshAlign(announce = false)
        if (ui.diffOn) recomputeDiff(pulse = false)
    }

    fun select(id: Long) {
        if (id == ui.selectedId) return
        val item = ui.generated.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { ImageDecode.decode(File(item.path), DISPLAY_EDGE) } ?: return@launch
            selectedBitmap = bitmap
            ui = ui.copy(selectedId = id, selectedImage = bitmap.asImageBitmap())
            if (ui.autoAlign) refreshAlign(announce = false) else ui = ui.copy(alignment = Alignment.Identity)
            if (ui.diffOn) recomputeDiff(pulse = true)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { repository.setFavorite(comparisonId, !ui.favorite) }
    }

    fun setBest(generatedId: Long? = null) {
        val id = generatedId ?: ui.selectedId ?: return
        viewModelScope.launch { repository.setBest(comparisonId, id) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(comparisonId)
            onDeleted()
        }
    }

    fun setAutoAlign(enabled: Boolean) {
        ui = ui.copy(autoAlign = enabled)
        alignToken++
        alignJob?.cancel()
        if (!enabled) {
            ui = ui.copy(alignment = Alignment.Identity, alignBusy = false)
            if (ui.diffOn) recomputeDiff(pulse = false)
            return
        }
        refreshAlign(announce = true)
    }

    fun setDiff(enabled: Boolean) {
        ui = ui.copy(diffOn = enabled)
        if (!enabled) {
            diffToken++
            diffJob?.cancel()
            ui = ui.copy(diffImage = null, diffBusy = false)
            return
        }
        recomputeDiff(pulse = true)
    }

    private suspend fun apply(item: ComparisonWithImages) {
        val images = item.orderedImages
        val key = item.comparison.originalPath + "|" + images.joinToString { it.path }
        if (key == loadedKey && ui.originalImage != null) {
            ui = ui.copy(
                favorite = item.comparison.favorite,
                bestId = item.comparison.bestGeneratedId,
                category = item.comparison.category,
                loading = false,
                missing = false,
            )
            return
        }
        loadedKey = key
        ui = ui.copy(loading = true, missing = false, category = item.comparison.category)
        val original = withContext(Dispatchers.IO) {
            ImageDecode.decode(File(item.comparison.originalPath), DISPLAY_EDGE)
        }
        val thumbs = HashMap<Long, ImageBitmap?>()
        for (image in images) {
            val thumb = withContext(Dispatchers.IO) { ImageDecode.decode(File(image.path), 256) }
            thumbs[image.id] = thumb?.asImageBitmap()
        }
        val selected = images.firstOrNull { it.id == item.comparison.bestGeneratedId } ?: images.firstOrNull()
        val selectedBmp = selected?.let { entity ->
            withContext(Dispatchers.IO) { ImageDecode.decode(File(entity.path), DISPLAY_EDGE) }
        }
        originalBitmap = original
        selectedBitmap = selectedBmp
        originalFace = null
        originalFaceLoaded = false
        generatedFaces.clear()
        generatedFaceLoaded.clear()
        ui = ui.copy(
            loading = false,
            favorite = item.comparison.favorite,
            originalWidth = item.comparison.originalWidth,
            originalHeight = item.comparison.originalHeight,
            originalBytes = item.comparison.originalBytes,
            originalSharpness = item.comparison.originalSharpness,
            generated = images.map { it.toThumb(thumbs[it.id]) },
            selectedId = selected?.id,
            bestId = item.comparison.bestGeneratedId,
            originalImage = original?.asImageBitmap(),
            selectedImage = selectedBmp?.asImageBitmap(),
            alignment = Alignment.Identity,
            diffImage = null,
        )
        if (ui.autoAlign) refreshAlign(announce = false)
        if (ui.diffOn) recomputeDiff(pulse = false)
    }

    private fun refreshAlign(announce: Boolean) {
        val original = originalBitmap
        val generated = selectedBitmap
        val selectedId = ui.selectedId
        val token = ++alignToken
        if (!ui.autoAlign || original == null || generated == null || selectedId == null || slotW < 2 || slotH < 2) {
            ui = ui.copy(alignBusy = false, alignment = if (ui.autoAlign) ui.alignment else Alignment.Identity)
            return
        }
        alignJob?.cancel()
        alignJob = viewModelScope.launch {
            ui = ui.copy(alignBusy = true)
            val origFace = if (originalFaceLoaded) {
                originalFace
            } else {
                detect(original).also {
                    originalFace = it
                    originalFaceLoaded = true
                }
            }
            val genFace = if (selectedId in generatedFaceLoaded) {
                generatedFaces[selectedId]
            } else {
                detect(generated).also {
                    generatedFaces[selectedId] = it
                    generatedFaceLoaded += selectedId
                }
            }
            if (token != alignToken || !ui.autoAlign) return@launch
            val alignment = AlignMath.alignFaces(
                viewW = slotW.toFloat(),
                viewH = slotH.toFloat(),
                originalW = original.width.toFloat(),
                originalH = original.height.toFloat(),
                generatedW = generated.width.toFloat(),
                generatedH = generated.height.toFloat(),
                originalFace = origFace,
                generatedFace = genFace,
            )
            ui = ui.copy(alignment = alignment, alignBusy = false)
            if (announce && alignment.isIdentity) {
                message = "未检测到人脸，已按尺寸居中"
            }
            if (ui.diffOn) recomputeDiff(pulse = false)
        }
    }

    private suspend fun detect(bitmap: Bitmap): FaceBox? = withContext(Dispatchers.Default) {
        runCatching { aligner.largestFace(bitmap) }.getOrNull()
    }

    private fun recomputeDiff(pulse: Boolean) {
        val original = originalBitmap
        val generated = selectedBitmap
        val token = ++diffToken
        if (!ui.diffOn || original == null || generated == null || slotW < 2 || slotH < 2) {
            ui = ui.copy(diffBusy = false)
            return
        }
        val alignment = ui.alignment
        val viewW = slotW
        val viewH = slotH
        diffJob?.cancel()
        diffJob = viewModelScope.launch {
            ui = ui.copy(diffBusy = true)
            val mask = withContext(Dispatchers.Default) {
                runCatching { DiffRenderer.render(original, generated, alignment, viewW, viewH) }.getOrNull()
            }
            if (token != diffToken || !ui.diffOn) return@launch
            ui = ui.copy(
                diffBusy = false,
                diffImage = mask?.asImageBitmap(),
                diffGeneration = if (pulse) ui.diffGeneration + 1 else ui.diffGeneration,
            )
        }
    }

    private fun GeneratedImageEntity.toThumb(thumb: ImageBitmap?) = GeneratedThumb(
        id = id,
        path = path,
        width = width,
        height = height,
        bytes = bytes,
        sharpness = sharpness,
        thumb = thumb,
    )

    class Factory(private val id: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CompareViewModel(Graph.repository, id) as T
        }
    }

    companion object {
        const val DISPLAY_EDGE = 2048
    }
}
