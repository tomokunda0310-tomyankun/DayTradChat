// /app/src/main/java/com/daytradchat/papa/data/ChatMessage.kt
// ver 1.00-00
package com.daytradchat.papa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String? = null,
    val body: String? = null,
    val sentAt: String? = null,
    val serverTime: String? = null,
    val rawJson: String,
    val code: String? = null,
    val signalType: String? = null,
    val signalScore: Int? = null,
    val marketBias: String? = null,
    val reasonShort: String? = null,
    val price: Double? = null,
    val displayOrderTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
