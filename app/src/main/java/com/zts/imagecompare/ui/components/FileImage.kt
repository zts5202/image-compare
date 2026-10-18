package com.zts.imagecompare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.zts.imagecompare.image.ImageDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileImage(
    path: String,
    maxEdge: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var image by remember(path, maxEdge) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path, maxEdge) {
        val bitmap = withContext(Dispatchers.IO) { ImageDecode.decode(File(path), maxEdge) }
        image = bitmap?.asImageBitmap()
    }
    val current = image
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier.background(Color(0xFF121316)))
    }
}
