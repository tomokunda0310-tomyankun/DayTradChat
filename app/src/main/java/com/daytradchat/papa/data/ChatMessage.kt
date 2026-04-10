//app/src/main/java/com/daytradchat/papa/data/ChatMessage.kt
//ver 1.00-16

package com.daytradchat.papa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String? = "",
    val rawJson: String? = "",
    val title: String? = "",
    val body: String? = "",
    val code: String? = "",
    val signalType: String? = "",
    val score: Int? = 0,
    val price: Double? = 0.0,
    val vwap: Double? = 0.0,
    val boardOver: Long? = 0L,
    val boardUnder: Long? = 0L,
    val changeRate: Double? = 0.0,
    val reasonShort: String? = "",
    val reasonDetail: String? = "",
    val sentAt: String? = "",
    val receivedAt: Long? = 0L
)