package com.zts.imagecompare.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zts.imagecompare.ui.compare.CompareScreen
import com.zts.imagecompare.ui.create.CreateComparisonSheet
import com.zts.imagecompare.ui.home.HomeListMode
import com.zts.imagecompare.ui.home.HomeScreen
import com.zts.imagecompare.ui.home.HomeViewModel
import com.zts.imagecompare.ui.settings.SettingsScreen
import com.zts.imagecompare.ui.theme.Accent
import com.zts.imagecompare.ui.theme.AccentPressed
import com.zts.imagecompare.ui.theme.Background
import com.zts.imagecompare.ui.theme.Border
import com.zts.imagecompare.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private enum class MainTab(val label: String) {
    Home("首页"),
    Mine("我的对比"),
    Favorites("收藏"),
    Settings("设置"),
}

@Composable
fun AppRoot() {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory())
    val items by homeViewModel.items.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var comparisonId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    androidx.activity.compose.BackHandler(enabled = comparisonId != null) { comparisonId = null }
    androidx.activity.compose.BackHandler(enabled = comparisonId == null && searchOpen) {
        searchOpen = false
        query = ""
    }
    androidx.activity.compose.BackHandler(enabled = comparisonId == null && showCreate) { showCreate = false }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Background, tonalElevation = 0.dp) {
                MainTab.entries.forEach { item ->
                    val highlighted = tab == item
                    NavigationBarItem(
                        selected = highlighted,
                        onClick = {
                            comparisonId = null
                            tab = item
                        },
                        icon = {
                            when (item) {
                                MainTab.Home -> Icon(Icons.Filled.Home, contentDescription = item.label)
                                MainTab.Mine -> LayersIcon(
                                    if (highlighted) Accent else TextSecondary,
                                    Modifier.size(24.dp),
                                )
                                MainTab.Favorites -> Icon(Icons.Filled.Star, contentDescription = item.label)
                                MainTab.Settings -> Icon(Icons.Filled.Settings, contentDescription = item.label)
                            }
                        },
                        label = { Text(item.label, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (comparisonId == null && tab != MainTab.Settings) {
                NewCompareFab(onClick = { showCreate = true })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Background)) {
            val openId = comparisonId
            if (openId != null) {
                CompareScreen(
                    comparisonId = openId,
                    onBack = { comparisonId = null },
                    onMessage = ::message,
                )
            } else when (tab) {
                MainTab.Home -> HomeScreen(
                    title = "图像对比",
                    mode = HomeListMode.All,
                    items = items,
                    query = query,
                    searchOpen = searchOpen,
                    category = category,
                    showFilters = true,
                    onQueryChange = { query = it },
                    onSearchOpenChange = { searchOpen = it },
                    onCategoryChange = { category = it },
                    onOpen = { comparisonId = it },
                    onFavorite = homeViewModel::setFavorite,
                    onDelete = homeViewModel::delete,
                )
                MainTab.Mine -> HomeScreen(
                    title = "我的对比",
                    mode = HomeListMode.All,
                    items = items,
                    query = query,
                    searchOpen = searchOpen,
                    category = null,
                    showFilters = false,
                    onQueryChange = { query = it },
                    onSearchOpenChange = { searchOpen = it },
                    onCategoryChange = {},
                    onOpen = { comparisonId = it },
                    onFavorite = homeViewModel::setFavorite,
                    onDelete = homeViewModel::delete,
                )
                MainTab.Favorites -> HomeScreen(
                    title = "收藏",
                    mode = HomeListMode.Favorites,
                    items = items,
                    query = query,
                    searchOpen = searchOpen,
                    category = null,
                    showFilters = false,
                    onQueryChange = { query = it },
                    onSearchOpenChange = { searchOpen = it },
                    onCategoryChange = {},
                    onOpen = { comparisonId = it },
                    onFavorite = homeViewModel::setFavorite,
                    onDelete = homeViewModel::delete,
                )
                MainTab.Settings -> SettingsScreen(onCleared = { message("已清除") })
            }
        }
    }

    if (showCreate) {
        CreateComparisonSheet(
            onDismiss = { showCreate = false },
            onCreated = { id ->
                showCreate = false
                tab = MainTab.Mine
                comparisonId = id
            },
        )
    }
}

@Composable
private fun NewCompareFab(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.42f to Accent.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f,
                    ),
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (pressed) AccentPressed else Accent)
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建对比", tint = Color.Black)
            }
        }
        Text(
            "新建对比",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.offset(y = (-8).dp),
        )
    }
}
