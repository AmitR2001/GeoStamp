package com.example.geostamp.domain.metadata

import java.util.Locale

object FileSizes {
    /** 1536 -> "1.5 KB", 5242880 -> "5.0 MB". */
    fun human(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }
}
