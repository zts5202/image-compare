package com.zts.imagecompare.ui.crop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material Symbols glyph "crop", filled, 24dp. */
val CropSymbol: ImageVector = ImageVector.Builder(
    name = "Crop",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(17f, 15f)
        horizontalLineTo(19f)
        verticalLineTo(7f)
        curveTo(19f, 5.9f, 18.1f, 5f, 17f, 5f)
        horizontalLineTo(9f)
        verticalLineTo(7f)
        horizontalLineTo(17f)
        verticalLineTo(15f)
        moveTo(7f, 17f)
        verticalLineTo(1f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineTo(1f)
        verticalLineTo(7f)
        horizontalLineTo(5f)
        verticalLineTo(17f)
        curveTo(5f, 18.1f, 5.9f, 19f, 7f, 19f)
        horizontalLineTo(17f)
        verticalLineTo(23f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(23f)
        verticalLineTo(17f)
        horizontalLineTo(7f)
        close()
    }
}.build()
