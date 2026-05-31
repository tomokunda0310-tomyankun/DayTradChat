//app/src/main/java/com/daytradchat/papa/model/UiModels.kt
//ver 2.18-01
package com.daytradchat.papa.model

data class SignalCardUiModel(
    val slotId: String,
    val code: String,
    val name: String,
    val signalType: String? = null,
    val score: Int,
    val price: Double,
    val openPrice: Double? = 0.0,
    val open_price: Double? = 0.0,
    val profitText: String? = null,
    val signal_score: Int? = 0,
    val changeRate: Double,
    val reasonShort: String,
    val updatedAt: String,
    val isEmpty: Boolean,

    // ★ 判定区分（1〜5 / null）
    val judgeType: Int? = null,

    // ★ トレンド（-1:下降, 0:横ばい, 1:上昇）
    val trend: Int = 0
)

data class LogLineUiModel(
    val id: String,
    val text: String
)
