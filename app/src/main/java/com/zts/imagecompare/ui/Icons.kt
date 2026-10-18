package com.zts.imagecompare.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun LayersIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(w * 0.12f, w * 0.12f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.28f),
            size = Size(w * 0.64f, h * 0.56f),
            cornerRadius = radius,
            style = Stroke(width = w * 0.08f),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.16f),
            size = Size(w * 0.64f, h * 0.5f),
            cornerRadius = radius,
        )
    }
}

@Composable
fun CrownIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.72f)
            lineTo(w * 0.12f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.56f)
            lineTo(w * 0.5f, h * 0.22f)
            lineTo(w * 0.68f, h * 0.56f)
            lineTo(w * 0.88f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.72f)
            close()
        }
        drawPath(path, tint)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.74f),
            size = Size(w * 0.76f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )
    }
}

@Composable
fun SparkleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.42f
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + r * 0.18f, c.y - r * 0.18f)
            lineTo(c.x + r, c.y)
            lineTo(c.x + r * 0.18f, c.y + r * 0.18f)
            lineTo(c.x, c.y + r)
            lineTo(c.x - r * 0.18f, c.y + r * 0.18f)
            lineTo(c.x - r, c.y)
            lineTo(c.x - r * 0.18f, c.y - r * 0.18f)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun TargetIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.38f
        drawCircle(tint, radius = r, center = c, style = Stroke(width = size.minDimension * 0.08f))
        drawCircle(tint, radius = size.minDimension * 0.12f, center = c)
    }
}

@Composable
fun ChevronPair(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        fun chevron(cx: Float, dir: Float) {
            val path = Path().apply {
                moveTo(cx + dir * w * 0.08f, h * 0.28f)
                lineTo(cx - dir * w * 0.08f, h * 0.5f)
                lineTo(cx + dir * w * 0.08f, h * 0.72f)
            }
            drawPath(path, tint, style = stroke)
        }
        chevron(w * 0.36f, 1f)
        chevron(w * 0.64f, -1f)
    }
}
