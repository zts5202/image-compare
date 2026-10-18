package com.zts.imagecompare.ui.compare

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zts.imagecompare.core.Alignment as FaceAlignment
import com.zts.imagecompare.core.Zoom
import com.zts.imagecompare.core.ZoomMath
import com.zts.imagecompare.core.formatBytes
import com.zts.imagecompare.ui.ChevronPair
import com.zts.imagecompare.ui.CrownIcon
import com.zts.imagecompare.ui.SparkleIcon
import com.zts.imagecompare.ui.TargetIcon
import com.zts.imagecompare.ui.theme.Accent
import com.zts.imagecompare.ui.theme.Background
import com.zts.imagecompare.ui.theme.Border
import com.zts.imagecompare.ui.theme.Card
import com.zts.imagecompare.ui.theme.Crown
import com.zts.imagecompare.ui.theme.Motion
import com.zts.imagecompare.ui.theme.TextPrimary
import com.zts.imagecompare.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class CompareMode(val label: String) {
    SideBySide("并排"),
    Split("分割"),
    Hold("按住对比"),
    Overlay("叠加"),
    Flicker("闪烁"),
}

@Composable
fun CompareScreen(
    comparisonId: Long,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: CompareViewModel = viewModel(
        key = "compare-$comparisonId",
        factory = CompareViewModel.Factory(comparisonId),
    ),
) {
    val ui = viewModel.ui
    LaunchedEffect(ui.missing) {
        if (ui.missing) onBack()
    }
    LaunchedEffect(viewModel.message) {
        val text = viewModel.message ?: return@LaunchedEffect
        onMessage(text)
        viewModel.consumeMessage()
    }

    var mode by remember { mutableStateOf(CompareMode.Split) }
    var split by remember { mutableFloatStateOf(0.5f) }
    var zoom by remember { mutableStateOf(Zoom()) }
    var opacity by remember { mutableFloatStateOf(0.5f) }
    var holding by remember { mutableStateOf(false) }
    var flickerOn by remember { mutableStateOf(false) }
    var flickerOriginal by remember { mutableStateOf(false) }
    var loupe by remember { mutableStateOf<LoupeUi?>(null) }
    var loupePinned by remember { mutableStateOf(false) }
    var loupeSession by remember { mutableIntStateOf(0) }
    var boxW by remember { mutableFloatStateOf(0f) }
    var boxH by remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val modeState = rememberUpdatedState(mode)
    val splitState = rememberUpdatedState(split)
    val zoomState = rememberUpdatedState(zoom)

    LaunchedEffect(mode, flickerOn) {
        if (mode != CompareMode.Flicker || !flickerOn) {
            flickerOriginal = false
            if (mode != CompareMode.Flicker) flickerOn = false
            return@LaunchedEffect
        }
        var showOriginal = false
        while (true) {
            showOriginal = !showOriginal
            flickerOriginal = showOriginal
            delay(Motion.FlickerMillis)
        }
    }

    LaunchedEffect(mode, boxW, boxH) {
        if (boxW >= 2f && boxH >= 2f) {
            val slotW = if (mode == CompareMode.SideBySide) (boxW / 2f).roundToInt() else boxW.roundToInt()
            viewModel.onSlotSize(slotW, boxH.roundToInt())
        }
    }

    fun loupeAt(position: Offset): LoupeUi {
        val (cx, cy) = ZoomMath.contentPoint(zoom, position.x, position.y)
        val half = boxW / 2f
        val slotW = if (mode == CompareMode.SideBySide) half else boxW
        val localX = if (mode == CompareMode.SideBySide) {
            if (cx < half) cx else cx - half
        } else {
            cx
        }
        return LoupeUi(
            point = Offset(localX.coerceIn(0f, slotW), cy.coerceIn(0f, boxH)),
            anchor = position,
            slotW = slotW,
            slotH = boxH,
        )
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text(
                "对比",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = TextPrimary)
                }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    containerColor = Card,
                ) {
                    DropdownMenuItem(
                        text = { Text(if (ui.favorite) "取消收藏" else "收藏", color = TextPrimary) },
                        onClick = {
                            menu = false
                            viewModel.toggleFavorite()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("设为最佳", color = TextPrimary) },
                        onClick = {
                            menu = false
                            viewModel.setBest()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = Color(0xFFFF6B6B)) },
                        onClick = {
                            menu = false
                            confirmDelete = true
                        },
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompareMode.entries.forEach { item ->
                val selected = item == mode
                Text(
                    text = item.label,
                    color = if (selected) Color.Black else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) Accent else Color.Transparent)
                        .clickable {
                            mode = item
                            loupe = null
                            loupePinned = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged {
                    boxW = it.width.toFloat()
                    boxH = it.height.toFloat()
                }
                .pointerInput(Unit) {
                    compareGestures(
                        modeOf = { modeState.value },
                        splitOf = { splitState.value },
                        zoomedOf = { zoomState.value.scale > 1.01f },
                        onSplit = { split = it },
                        onZoom = { pan, z, centroid ->
                            zoom = ZoomMath.apply(
                                current = zoom,
                                panX = pan.x,
                                panY = pan.y,
                                zoom = z,
                                centroidX = centroid.x,
                                centroidY = centroid.y,
                                viewW = boxW,
                                viewH = boxH,
                            )
                        },
                        onTap = { position ->
                            when (mode) {
                                CompareMode.Flicker -> flickerOn = !flickerOn
                                CompareMode.Hold -> Unit
                                else -> {
                                    if (loupePinned) {
                                        loupe = null
                                        loupePinned = false
                                    } else if (boxW > 0f) {
                                        loupe = loupeAt(position)
                                        loupePinned = true
                                        loupeSession++
                                    }
                                }
                            }
                        },
                        onLoupeBegin = { position ->
                            if (boxW > 0f) {
                                val wasHidden = loupe == null
                                loupe = loupeAt(position)
                                if (wasHidden) loupeSession++
                            }
                        },
                        onLoupeMove = { position ->
                            if (boxW > 0f) loupe = loupeAt(position)
                        },
                        onLoupeEnd = {
                            if (!loupePinned) loupe = null
                        },
                        onHoldChanged = { holding = it },
                    )
                },
        ) {
            if (ui.loading || ui.originalImage == null || ui.selectedImage == null) {
                CircularProgressIndicator(
                    color = Accent,
                    modifier = Modifier.align(Alignment.Center),
                    strokeWidth = 2.dp,
                )
            } else {
                val original = ui.originalImage
                val generated = ui.selectedImage
                Crossfade(
                    targetState = mode,
                    animationSpec = tween(Motion.ModeFadeMillis),
                    label = "compare-mode",
                    modifier = Modifier.fillMaxSize(),
                ) { current ->
                    CompareCanvas(
                        mode = current,
                        original = original,
                        generated = generated,
                        alignment = ui.alignment,
                        zoom = zoom,
                        split = split,
                        opacity = opacity,
                        showOriginal = when (current) {
                            CompareMode.Hold -> holding
                            CompareMode.Flicker -> flickerOriginal
                            else -> false
                        },
                        diff = ui.diffImage,
                        diffGeneration = ui.diffGeneration,
                    )
                }
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToggleChip(
                        label = "差异高亮",
                        on = ui.diffOn,
                        busy = ui.diffBusy,
                        icon = { SparkleIcon(it, Modifier.size(14.dp)) },
                        onClick = { viewModel.setDiff(!ui.diffOn) },
                    )
                    ToggleChip(
                        label = "自动对齐",
                        on = ui.autoAlign,
                        busy = ui.alignBusy,
                        icon = { TargetIcon(it, Modifier.size(14.dp)) },
                        onClick = { viewModel.setAutoAlign(!ui.autoAlign) },
                    )
                }
                val activeLoupe = loupe
                if (activeLoupe != null) {
                    LoupeOverlay(
                        loupe = activeLoupe,
                        session = loupeSession,
                        original = original,
                        generated = generated,
                        alignment = ui.alignment,
                        boxWidth = boxW,
                        boxHeight = boxH,
                    )
                }
                if (mode == CompareMode.Hold || mode == CompareMode.Flicker) {
                    Text(
                        text = when (mode) {
                            CompareMode.Hold -> "按住查看原图"
                            CompareMode.Flicker -> if (flickerOn) "点按停止" else "点按开始闪烁"
                            else -> ""
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        val selected = ui.generated.firstOrNull { it.id == ui.selectedId }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("清晰度", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${ui.originalSharpness}",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(" vs ", color = TextSecondary, fontSize = 16.sp)
                Text(
                    "${selected?.sharpness ?: 0}",
                    color = Accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Border),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(((selected?.sharpness ?: 0) / 100f).coerceIn(0f, 1f))
                        .background(Accent),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${ui.originalWidth}×${ui.originalHeight} · ${formatBytes(ui.originalBytes)}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${selected?.width ?: 0}×${selected?.height ?: 0} · ${formatBytes(selected?.bytes ?: 0)} ",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
                Text("生成图", color = Accent, fontSize = 12.sp)
            }
        }

        if (mode == CompareMode.Overlay) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("透明度 ${(opacity * 100).roundToInt()}%", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Accent,
                        activeTrackColor = Accent,
                        inactiveTrackColor = Border,
                    ),
                )
            }
        }

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        ) {
            items(ui.generated, key = { it.id }) { item ->
                Thumb(
                    item = item,
                    selected = item.id == ui.selectedId,
                    best = item.id == ui.bestId,
                    onClick = { viewModel.select(item.id) },
                    onLongClick = { viewModel.setBest(item.id) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Card,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("删除这组对比？") },
            text = { Text("图片也会从本机移除。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("删除", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun CompareCanvas(
    mode: CompareMode,
    original: ImageBitmap,
    generated: ImageBitmap,
    alignment: FaceAlignment,
    zoom: Zoom,
    split: Float,
    opacity: Float,
    showOriginal: Boolean,
    diff: ImageBitmap?,
    diffGeneration: Int,
) {
    when (mode) {
        CompareMode.SideBySide -> {
            Row(Modifier.fillMaxSize().zoomLayer(zoom)) {
                Image(original, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Fit)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Image(
                        generated,
                        null,
                        Modifier.fillMaxSize().alignLayer(alignment),
                        contentScale = ContentScale.Fit,
                    )
                    if (diff != null) DiffOverlay(diff, diffGeneration, Modifier.fillMaxSize())
                }
            }
        }
        CompareMode.Split -> {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(FractionClip(0f, split))
                        .zoomLayer(zoom),
                ) {
                    Image(original, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(FractionClip(split, 1f))
                        .zoomLayer(zoom),
                ) {
                    Image(
                        generated,
                        null,
                        Modifier.fillMaxSize().alignLayer(alignment),
                        contentScale = ContentScale.Fit,
                    )
                    if (diff != null) DiffOverlay(diff, diffGeneration, Modifier.fillMaxSize())
                }
                SplitHandle(split)
            }
        }
        CompareMode.Overlay -> {
            Box(Modifier.fillMaxSize().zoomLayer(zoom)) {
                Image(original, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Image(
                    generated,
                    null,
                    Modifier.fillMaxSize().alignLayer(alignment).graphicsLayer { alpha = opacity },
                    contentScale = ContentScale.Fit,
                )
                if (diff != null) DiffOverlay(diff, diffGeneration, Modifier.fillMaxSize())
            }
        }
        CompareMode.Hold, CompareMode.Flicker -> {
            Box(Modifier.fillMaxSize().zoomLayer(zoom)) {
                if (showOriginal) {
                    Image(original, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    Image(
                        generated,
                        null,
                        Modifier.fillMaxSize().alignLayer(alignment),
                        contentScale = ContentScale.Fit,
                    )
                }
                if (diff != null) DiffOverlay(diff, diffGeneration, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SplitHandle(split: Float) {
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                width = it.width
                height = it.height
            },
    ) {
        Box(
            Modifier
                .offset { IntOffset((width * split).roundToInt() - 1.dp.roundToPx(), 0) }
                .width(2.dp)
                .fillMaxHeight()
                .background(Accent),
        )
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (width * split).roundToInt() - 17.dp.roundToPx(),
                        height / 2 - 17.dp.roundToPx(),
                    )
                }
                .size(34.dp)
                .background(Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ChevronPair(Color.White, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun DiffOverlay(image: ImageBitmap, generation: Int, modifier: Modifier) {
    val alpha = remember { Animatable(0.78f) }
    LaunchedEffect(generation) {
        if (generation == 0) return@LaunchedEffect
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(220))
        alpha.animateTo(0.78f, tween(420))
    }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.graphicsLayer { this.alpha = alpha.value },
    )
}

@Composable
private fun ToggleChip(
    label: String,
    on: Boolean,
    busy: Boolean,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
) {
    val tint = if (on) Accent else TextPrimary
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, if (on) Accent else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(12.dp), color = Accent, strokeWidth = 1.5.dp)
        } else {
            icon(tint)
        }
        Text(label, color = tint, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Thumb(
    item: GeneratedThumb,
    selected: Boolean,
    best: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width = 72.dp, height = 86.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        val thumb = item.thumb
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = "生成图",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 2.dp,
                        color = if (selected) Accent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                    ),
            )
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Card),
            )
        }
        if (best) {
            CrownIcon(
                Crown,
                Modifier.align(Alignment.TopCenter).size(16.dp),
            )
        }
    }
}

private fun Modifier.zoomLayer(zoom: Zoom): Modifier = graphicsLayer {
    transformOrigin = TransformOrigin(0f, 0f)
    scaleX = zoom.scale
    scaleY = zoom.scale
    translationX = zoom.offsetX
    translationY = zoom.offsetY
}

private fun Modifier.alignLayer(alignment: FaceAlignment): Modifier = graphicsLayer {
    transformOrigin = TransformOrigin(alignment.pivotXFraction, alignment.pivotYFraction)
    scaleX = alignment.scale
    scaleY = alignment.scale
    translationX = alignment.translationX
    translationY = alignment.translationY
}

private class FractionClip(private val start: Float, private val end: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Rectangle(Rect(size.width * start, 0f, size.width * end, size.height))
    }
}
