//app/src/main/java/com/daytradchat/papa/network/SystemLogStore.kt
//ver 2.13-00
package com.daytradchat.papa.network

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemLogStore(private val context: Context) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.JAPAN)

    private fun todayKey(): String = dateFormat.format(Date())

    private fun logFileFor(key: String): File {
        return File(context.filesDir, "systemlog_$key.txt")
    }

    fun cleanupOldFiles() {
        val today = todayKey()
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("systemlog_") && !file.name.contains(today)) {
                runCatching { file.delete() }
            }
        }
    }

    fun append(line: String) {
        cleanupOldFiles()
        val file = logFileFor(todayKey())
        file.appendText(line + "\n", Charsets.UTF_8)
    }

    fun loadToday(): List<String> {
        cleanupOldFiles()
        val file = logFileFor(todayKey())
        if (!file.exists()) return emptyList()
        return file.readLines(Charsets.UTF_8)
            .filter { it.isNotBlank() }
    }
}
