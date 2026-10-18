package com.zts.imagecompare.ui.crop

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.SideEffect
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.zts.imagecompare.core.AspectPreset
import com.zts.imagecompare.core.CropDecision
import com.zts.imagecompare.core.CropInput
import com.zts.imagecompare.core.CropMath
import com.zts.imagecompare.core.CropSession
import com.zts.imagecompare.core.CropSpec
import com.zts.imagecompare.core.CropSnapshot
import com.zts.imagecompare.image.ImageDecode
import com.zts.imagecompare.ui.theme.Accent
import com.zts.imagecompare.ui.theme.Background
import com.zts.imagecompare.ui.theme.Border
import com.zts.imagecompare.ui.theme.Card
import com.zts.imagecompare.ui.theme.TextPrimary
import com.zts.imagecompare.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

data class CropEditorTarget(
    val key: String,
    val label: String,
    val file: File,
    val imageW: Int,
    val imageH: Int,
    val saved: CropSpec? = null,
)

private class CropBridge {
    var onWindow: (com.zts.imagecompare.core.NormRect) -> Unit = {}
    var view: CropImageView? = null
    var suppress: Int = 0
    var bitmap: android.graphics.Bitmap? = null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrameCropDialog(
    targets: List<CropEditorTarget>,
    startIndex: Int,
    saving: Boolean,
    error: String?,
    onConfirm: (List<CropDecision>) -> Unit,
    onDismiss: () -> Unit,
) {
    val session = remember(targets.joinToString { it.key + ":" + it.file.absolutePath }) {
        CropSession(
            inputs = targets.map { CropInput(it.key, it.label, it.imageW, it.imageH, it.saved) },
            startIndex = startIndex,
        )
    }
    var snap by remember { mutableStateOf(session.snapshot()) }
    val visibility = remember { MutableTransitionState(false) }
    var started by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val bridge = remember { CropBridge() }
    val previewAlpha = remember { Animatable(1f) }
    val files = remember(targets) { targets.associate { it.key to it.file } }

    LaunchedEffect(Unit) {
        visibility.targetState = true
        started = true
    }
    LaunchedEffect(visibility.currentState, visibility.targetState, started) {
        if (started && !visibility.targetState && !visibility.currentState) onDismiss()
    }

    fun edit(block: () -> Unit) {
        if (saving) return
        block()
        snap = session.snapshot()
        revision++
    }

    fun readLive() {
        val view = bridge.view ?: return
        val rect = view.cropRect ?: return
        val whole = view.wholeImageRect ?: return
        if (whole.width() <= 1 || whole.height() <= 1) return
        session.onWindow(CropMath.normalize(rect.left, rect.top, rect.right, rect.bottom, whole.width(), whole.height()))
        snap = session.snapshot()
    }

    Dialog(
        onDismissRequest = { if (!saving) visibility.targetState = false },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                statusBarColor = android.graphics.Color.parseColor("#0E0F11")
                navigationBarColor = android.graphics.Color.parseColor("#0E0F11")
            }
        }
        BackHandler(enabled = !saving) { visibility.targetState = false }
        Box(Modifier.fillMaxSize().background(Background)) {
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 8 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 10 },
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Background)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    CropTopBar(
                        onBack = { if (!saving) visibility.targetState = false },
                        onRestore = { edit { session.restore() } },
                        enabled = !saving,
                    )
                    TargetRow(
                        snap = snap,
                        enabled = !saving,
                        onSelect = { index ->
                            readLive()
                            edit { session.select(index) }
                        },
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .graphicsLayer { alpha = previewAlpha.value },
                            factory = { context ->
                                CropImageView(context).apply {
                                    setBackgroundColor(0xFF0E0F11.toInt())
                                    setImageCropOptions(styledCropOptions(resources.displayMetrics.density))
                                    bridge.view = this
                                    setOnCropWindowChangedListener {
                                        if (bridge.suppress > 0) return@setOnCropWindowChangedListener
                                        val rect = cropRect ?: return@setOnCropWindowChangedListener
                                        val whole = wholeImageRect ?: return@setOnCropWindowChangedListener
                                        bridge.onWindow(
                                            CropMath.normalize(rect.left, rect.top, rect.right, rect.bottom, whole.width(), whole.height()),
                                        )
                                    }
                                }
                            },
                        )
                    }
                    CropControls(
                        snap = snap,
                        saving = saving,
                        error = error ?: loadError,
                        onPreset = { preset -> edit { session.setPreset(preset) } },
                        onSync = { enabled -> edit { session.setSync(enabled) } },
                        onUnify = { enabled -> edit { session.setUnify(enabled) } },
                        onConfirm = {
                            readLive()
                            onConfirm(session.decisions())
                        },
                    )
                }
            }
        }
    }

    bridge.onWindow = { norm ->
        session.onWindow(norm)
        snap = session.snapshot()
    }

    LaunchedEffect(snap.index, visibility.targetState) {
        if (!visibility.targetState) return@LaunchedEffect
        val target = snap.targets.getOrNull(snap.index) ?: return@LaunchedEffect
        val file = files[target.key] ?: return@LaunchedEffect
        previewAlpha.animateTo(0.45f, tween(90))
        val bitmap = withContext(Dispatchers.IO) { ImageDecode.decode(file, 2048) }
        if (!isActive) {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
            return@LaunchedEffect
        }
        var cropView = bridge.view
        var spins = 0
        while (cropView == null && spins < 20) {
            delay(16)
            cropView = bridge.view
            spins++
        }
        if (bitmap == null || cropView == null) {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
            loadError = "无法解码图片"
            previewAlpha.animateTo(1f, tween(160))
            return@LaunchedEffect
        }
        loadError = null
        val previous = bridge.bitmap
        bridge.suppress++
        cropView.setImageBitmap(bitmap)
        bridge.bitmap = bitmap
        if (previous != null && previous !== bitmap && !previous.isRecycled) previous.recycle()
        applyDraft(cropView, session.snapshot())
        cropView.post { bridge.suppress = (bridge.suppress - 1).coerceAtLeast(0) }
        previewAlpha.animateTo(1f, tween(200))
    }

    LaunchedEffect(revision) {
        if (revision == 0) return@LaunchedEffect
        val cropView = bridge.view ?: return@LaunchedEffect
        if (bridge.bitmap == null) return@LaunchedEffect
        bridge.suppress++
        applyDraft(cropView, session.snapshot())
        cropView.post { bridge.suppress = (bridge.suppress - 1).coerceAtLeast(0) }
    }

    DisposableEffect(bridge) {
        onDispose {
            val bitmap = bridge.bitmap
            bridge.bitmap = null
            bridge.view = null
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
    }
}

private fun applyDraft(view: CropImageView, snap: CropSnapshot) {
    val active = snap.active
    val source = snap.targets.first()
    val ratio = active.preset.ratio(active.imageW, active.imageH, source.imageW, source.imageH, snap.unify)
    if (ratio == null) view.clearAspectRatio() else view.setAspectRatio(ratio.first.coerceAtLeast(1), ratio.second.coerceAtLeast(1))
    val whole = view.wholeImageRect ?: return
    val pixels = CropMath.toPixels(active.norm, whole.width(), whole.height())
    view.cropRect = Rect(pixels.left, pixels.top, pixels.left + pixels.width, pixels.top + pixels.height)
}

private fun styledCropOptions(density: Float): CropImageOptions {
    val teal = 0xFF00E5B0.toInt()
    return CropImageOptions(
        borderLineColor = teal,
        borderCornerColor = teal,
        circleCornerFillColorHexValue = teal,
        guidelinesColor = 0x9900E5B0.toInt(),
        backgroundColor = 0xCC0E0F11.toInt(),
        borderLineThickness = 2.5f * density,
        borderCornerThickness = 4f * density,
        borderCornerLength = 22f * density,
        borderCornerOffset = 2f * density,
        guidelinesThickness = 1f * density,
        guidelines = CropImageView.Guidelines.ON,
        showProgressBar = false,
        multiTouchEnabled = true,
        centerMoveEnabled = true,
        autoZoomEnabled = true,
        initialCropWindowPaddingRatio = 0f,
        scaleType = CropImageView.ScaleType.FIT_CENTER,
        activityBackgroundColor = 0xFF0E0F11.toInt(),
        progressBarColor = teal,
        touchRadius = 28f * density,
        cropperLabelTextColor = teal,
    )
}

@Composable
private fun CropTopBar(onBack: () -> Unit, onRestore: () -> Unit, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = enabled) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
        }
        Icon(CropSymbol, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("裁剪", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        PressText(label = "还原", enabled = enabled, onClick = onRestore)
    }
}

@Composable
private fun TargetRow(snap: CropSnapshot, enabled: Boolean, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        snap.targets.forEachIndexed { index, target ->
            val selected = index == snap.index
            val color by animateColorAsState(if (selected) Accent else Card, tween(180), label = "target")
            val text = if (selected) Color.Black else TextSecondary
            PressSurface(enabled = enabled, onClick = { onSelect(index) }, shape = CircleShape) {
                Text(
                    target.label,
                    color = text,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier
                        .background(color, CircleShape)
                        .border(1.dp, if (selected) Accent else Border, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CropControls(
    snap: CropSnapshot,
    saving: Boolean,
    error: String?,
    onPreset: (AspectPreset) -> Unit,
    onSync: (Boolean) -> Unit,
    onUnify: (Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text("画幅", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectPreset.entries.forEach { preset ->
                val selected = snap.active.preset == preset
                val color by animateColorAsState(if (selected) Accent else Background, tween(180), label = "preset")
                PressSurface(enabled = !saving, onClick = { onPreset(preset) }, shape = CircleShape) {
                    Text(
                        preset.label,
                        color = if (selected) Color.Black else TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(color, CircleShape)
                            .border(1.dp, if (selected) Accent else Border, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("同步裁剪", snap.sync, !saving) { onSync(!snap.sync) }
            OptionChip("统一画幅", snap.unify, !saving) { onUnify(!snap.unify) }
        }
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Color(0xFFFF6B6B), fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        PressSurface(enabled = !saving, onClick = onConfirm, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (saving) Border else Accent, CircleShape)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在裁剪…", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("完成", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun OptionChip(label: String, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (on) Accent else Background, tween(180), label = label)
    PressSurface(enabled = enabled, onClick = onClick, shape = CircleShape) {
        Text(
            label,
            color = if (on) Color.Black else TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier
                .background(color, CircleShape)
                .border(1.dp, if (on) Accent else Border, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PressText(label: String, enabled: Boolean, onClick: () -> Unit) {
    PressSurface(enabled = enabled, onClick = onClick, shape = CircleShape) {
        Text(
            label,
            color = Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PressSurface(
    enabled: Boolean,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(90),
        label = "press",
    )
    Box(
        modifier
            .scale(scale)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}
