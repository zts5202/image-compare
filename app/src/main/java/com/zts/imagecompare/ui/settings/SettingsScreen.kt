package com.zts.imagecompare.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zts.imagecompare.BuildConfig
import com.zts.imagecompare.ui.theme.Accent
import com.zts.imagecompare.ui.theme.Background
import com.zts.imagecompare.ui.theme.Border
import com.zts.imagecompare.ui.theme.Card
import com.zts.imagecompare.ui.theme.TextPrimary
import com.zts.imagecompare.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onCleared: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory()),
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Background).padding(16.dp),
    ) {
        Text("设置", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            Text("关于", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "图像对比用于离线比较原图与生成图。图片通过系统相册选择，只保存在应用私有目录，不会上传。",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingsCard {
            Text("版本", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(BuildConfig.VERSION_NAME, color = Accent, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        SettingsCard {
            TextButton(onClick = { confirm = true }) {
                Text("清除全部数据", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B), fontSize = 14.sp)
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = Card,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("清除全部数据？") },
            text = { Text("所有对比和图片都会从本机删除，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.clearAll(onCleared)
                }) {
                    Text("清除", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Card)
            .border(1.dp, Border, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        content()
    }
}
