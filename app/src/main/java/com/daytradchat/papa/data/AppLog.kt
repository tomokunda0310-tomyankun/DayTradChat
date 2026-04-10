// /app/src/main/java/com/daytradchat/papa/data/AppLog.kt
// ver 1.00-00
package com.daytradchat.papa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
