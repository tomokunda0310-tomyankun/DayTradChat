// app/src/main/java/com/daytradchat/papa/model/TradeItem.kt
// ver 2.17-17
package com.daytradchat.papa.model

/**
 * 板情報やシグナルで表示する1銘柄分のデータモデル
 */
data class TradeItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val code: String,      // 銘柄コード (例: "9101")
    val name: String,      // 銘柄名
    val price: String,     // 現在値
    val change: String,    // 前日比
    val signalType: String // "LONG" or "SHORT" など
)
