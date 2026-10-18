package com.zts.imagecompare.ui.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.zts.imagecompare.core.AlignMath
import com.zts.imagecompare.core.Alignment
import com.zts.imagecompare.ui.theme.Background
import com.zts.imagecompare.ui.theme.Motion
import kotlin.math.roundToInt

data class LoupeUi(
    val point: Offset,
    val anchor: Offset,
    val slotW: Float,
    val slotH: Float,
)

@Composable
fun LoupeOverlay(
    loupe: LoupeUi,
    session: Int,
    original: ImageBitmap,
    generated: ImageBitmap,
    alignment: Alignment,
    boxWidth: Float,
    boxHeight: Float,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(session) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(Motion.LoupeMillis))
    }
    val loupeDp = 132.dp
    val loupePx = with(androidx.compose.ui.platform.LocalDensity.current) { loupeDp.toPx() }
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    var top = loupe.anchor.y - loupePx - gap
    if (top < 0f) top = loupe.anchor.y + gap
    var left = loupe.anchor.x - loupePx / 2f
    left = left.coerceIn(0f, (boxWidth - loupePx).coerceAtLeast(0f))
    top = top.coerceIn(0f, (boxHeight - loupePx).coerceAtLeast(0f))
    val scale = 0.86f + 0.14f * progress.value
    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(loupeDp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = progress.value
            }
            .border(2.dp, Color.White, CircleShape)
            .padding(2.dp)
            .clip(CircleShape),
    ) {
            LoupeCanvas(
            original = original,
            generated = generated,
            alignment = alignment,
            contentPoint = loupe.point,
            slotWidth = loupe.slotW,
            slotHeight = loupe.slotH,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LoupeCanvas(
    original: ImageBitmap,
    generated: ImageBitmap,
    alignment: Alignment,
    contentPoint: Offset,
    slotWidth: Float,
    slotHeight: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        drawRect(Background)
        val magnification = 2.7f
        val center = Offset(size.width / 2f, size.height / 2f)
        clipRect(right = size.width / 2f) {
            drawMapped(original, Alignment.Identity, contentPoint, slotWidth, slotHeight, magnification, center)
        }
        clipRect(left = size.width / 2f) {
            drawMapped(generated, alignment, contentPoint, slotWidth, slotHeight, magnification, center)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMapped(
    image: ImageBitmap,
    alignment: Alignment,
    contentPoint: Offset,
    slotWidth: Float,
    slotHeight: Float,
    magnification: Float,
    center: Offset,
) {
    val fit = AlignMath.fitCenter(image.width.toFloat(), image.height.toFloat(), slotWidth, slotHeight)
    val pivotX = alignment.pivotXFraction * slotWidth
    val pivotY = alignment.pivotYFraction * slotHeight
    val alignedLeft = (fit.left - pivotX) * alignment.scale + pivotX + alignment.translationX
    val alignedTop = (fit.top - pivotY) * alignment.scale + pivotY + alignment.translationY
    val alignedW = fit.width * alignment.scale
    val alignedH = fit.height * alignment.scale
    val destLeft = (alignedLeft - contentPoint.x) * magnification + center.x
    val destTop = (alignedTop - contentPoint.y) * magnification + center.y
    val destW = alignedW * magnification
    val destH = alignedH * magnification
    if (destW < 1f || destH < 1f) return
    drawImage(
        image = image,
        dstOffset = IntOffset(destLeft.roundToInt(), destTop.roundToInt()),
        dstSize = IntSize(destW.roundToInt().coerceAtLeast(1), destH.roundToInt().coerceAtLeast(1)),
    )
}
