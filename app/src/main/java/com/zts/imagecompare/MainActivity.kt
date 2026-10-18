package com.zts.imagecompare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zts.imagecompare.ui.AppRoot
import com.zts.imagecompare.ui.theme.ImageCompareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF0E0F11.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF0E0F11.toInt()),
        )
        setContent {
            ImageCompareTheme {
                AppRoot()
            }
        }
    }
}
