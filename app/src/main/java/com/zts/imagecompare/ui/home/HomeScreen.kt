package com.zts.imagecompare.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.imagecompare.data.Category
import com.zts.imagecompare.data.ComparisonWithImages
import com.zts.imagecompare.ui.CrownIcon
import com.zts.imagecompare.ui.components.FileImage
import com.zts.imagecompare.ui.theme.Accent
import com.zts.imagecompare.ui.theme.Background
import com.zts.imagecompare.ui.theme.Badge
import com.zts.imagecompare.ui.theme.Border
import com.zts.imagecompare.ui.theme.Card
import com.zts.imagecompare.ui.theme.Crown
import com.zts.imagecompare.ui.theme.Motion
import com.zts.imagecompare.ui.theme.TextPrimary
import com.zts.imagecompare.ui.theme.TextSecondary

enum class HomeListMode { All, Favorites }

@Composable
fun HomeScreen(
    title: String,
    mode: HomeListMode,
    items: List<ComparisonWithImages>,
    query: String,
    searchOpen: Boolean,
    category: String?,
    showFilters: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onOpen: (Long) -> Unit,
    onFavorite: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<ComparisonWithImages?>(null) }
    val filtered = items.filter { item ->
        val favoriteOk = mode != HomeListMode.Favorites || item.comparison.favorite
        val categoryOk = category == null || item.comparison.category == category
        val queryOk = query.isBlank() || item.comparison.category.contains(query.trim(), ignoreCase = true)
        favoriteOk && categoryOk && queryOk
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        TitleRow(
            title = title,
            query = query,
            searchOpen = searchOpen,
            onQueryChange = onQueryChange,
            onSearchOpenChange = onSearchOpenChange,
        )
        if (showFilters) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("全部", category == null) { onCategoryChange(null) }
                Category.labels.forEach { label ->
                    FilterChip(label, category == label) { onCategoryChange(label) }
                }
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        query.isNotBlank() -> "没有匹配的对比"
                        mode == HomeListMode.Favorites -> "还没有收藏"
                        else -> "还没有对比\n点按右下角新建"
                    },
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.comparison.id }) { item ->
                    ComparisonCard(
                        item = item,
                        onClick = { onOpen(item.comparison.id) },
                        onLongClick = { pendingDelete = item },
                        onFavorite = { onFavorite(item.comparison.id, !item.comparison.favorite) },
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Card,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("对比操作") },
            text = {
                Column {
                    TextButton(onClick = {
                        onFavorite(item.comparison.id, !item.comparison.favorite)
                        pendingDelete = null
                    }) {
                        Text(if (item.comparison.favorite) "取消收藏" else "收藏", color = TextPrimary)
                    }
                    TextButton(onClick = {
                        onDelete(item.comparison.id)
                        pendingDelete = null
                    }) {
                        Text("删除", color = ColorDelete)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("关闭", color = Accent)
                }
            },
        )
    }
}

private val ColorDelete = androidx.compose.ui.graphics.Color(0xFFFF6B6B)

@Composable
private fun TitleRow(
    title: String,
    query: String,
    searchOpen: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (searchOpen) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(Card)
                    .border(1.dp, Border, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("搜索分类", color = TextSecondary, fontSize = 16.sp)
                    }
                    inner()
                },
            )
            IconButton(onClick = {
                onQueryChange("")
                onSearchOpenChange(false)
            }) {
                Icon(Icons.Filled.Close, contentDescription = "关闭搜索", tint = TextSecondary)
            }
        } else {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onSearchOpenChange(true) }) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = TextPrimary)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) androidx.compose.ui.graphics.Color.Black else TextSecondary,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Accent else Card)
            .border(1.dp, if (selected) Accent else Border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComparisonCard(
    item: ComparisonWithImages,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.CardPressedScale else 1f,
        animationSpec = tween(90),
        label = "card-press",
    )
    val best = item.bestOrFirst()
    Column(
        Modifier
            .scale(scale)
            .clip(shape)
            .background(Card)
            .border(1.dp, Border, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().aspectRatio(1.25f).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxSize()) {
                FileImage(
                    path = item.comparison.originalPath,
                    maxEdge = 480,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                )
                Text(
                    text = "原图",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(CircleShape)
                        .background(Badge)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
                if (best != null) {
                    FileImage(
                        path = best.path,
                        maxEdge = 480,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    )
                }
                if (item.comparison.bestGeneratedId != null) {
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Badge)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CrownIcon(Crown, Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("最佳", color = Crown, fontSize = 12.sp)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.comparison.category,
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = " · ${item.images.size}张生成图",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (item.comparison.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (item.comparison.favorite) "取消收藏" else "收藏",
                tint = if (item.comparison.favorite) Accent else TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onFavorite),
            )
        }
    }
}
