package com.zts.imagecompare.core

import java.util.Locale

fun formatBytes(bytes: Long): String {
    val b = bytes.coerceAtLeast(0L)
    return when {
        b < 1024L -> "${b}B"
        b < 1024L * 1024L -> String.format(Locale.US, "%.1fKB", b / 1024.0)
        else -> String.format(Locale.US, "%.1fMB", b / (1024.0 * 1024.0))
    }
}
