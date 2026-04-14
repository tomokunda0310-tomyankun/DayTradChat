//app/src/main/java/com/daytradchat/papa/model/UiModels.kt
//ver 2.13-00
package com.daytradchat.papa.model

data class SignalCardUiModel(
    val slotId: String,
    val codeName: String,
    val priceText: String,
    val deltaText: String,
    val sub1: String,
    val sub2: String,
    val updatedAt: String,
    val signalType: String,
    val changeRate: Double,
    val isEmpty: Boolean = false
)

data class LogLineUiModel(
    val id: String,
    val text: String
)
