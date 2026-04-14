//app/src/main/java/com/daytradchat/papa/model/UiModels.kt
//ver 2.13-13

package com.daytradchat.papa.model

data class SignalCardUiModel(
    val slotId: String,
    val code: String,
    val name: String,
    val signalType: String,
    val score: Int,
    val price: Double,
    val changeRate: Double,
    val reasonShort: String,
    val updatedAt: String,
    val isEmpty: Boolean = false
)

data class LogLineUiModel(
    val id: String,
    val text: String
)

data class SignalHistoryUiModel(
    val time: String,
    val price: Double,
    val changeRate: Double,
    val score: Int,
    val reasonShort: String
)
