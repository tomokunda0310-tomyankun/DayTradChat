//app/src/main/java/com/daytradchat/papa/model/ServerMessages.kt
//ver 2.13-00
package com.daytradchat.papa.model

sealed class ServerMessage {
    data class ServerHello(
        val type: String,
        val server_time: String,
        val message: String,
        val port: Int
    ) : ServerMessage()

    data class Pong(
        val type: String,
        val server_time: String
    ) : ServerMessage()

    data class AckMessage(
        val type: String,
        val message: String,
        val original_type: String = "",
        val client_name: String? = null,
        val client_version: String? = null
    ) : ServerMessage()

    data class ErrorMessage(
        val type: String,
        val message: String,
        val raw: String? = null,
        val detail: String? = null
    ) : ServerMessage()

    data class SignalBatch(
        val type: String,
        val sent_at: String,
        val items: List<SignalItem>,
        val market: MarketItem? = null,
        val count: Int? = null
    ) : ServerMessage()
}

data class MarketItem(
    val type: String = "",
    val code: String = "",
    val name: String = "",
    val market: String = "",
    val captured_at: String = "",
    val price: Double? = null,
    val change_value: Double? = null,
    val change_rate: Double? = null,
    val prev_close: Double? = null,
    val price_time: String? = null,
    val source: String? = null
)

data class SignalItem(
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val code: String? = null,
    val name: String? = null,
    val market: String? = null,
    val captured_at: String? = null,
    val price: Double? = null,
    val change_value: Double? = null,
    val change_rate: Double? = null,
    val prev_close: Double? = null,
    val price_time: String? = null,
    val source: String? = null,
    val data: SignalData = SignalData()
)

data class SignalData(
    val code: String = "",
    val name: String = "",
    val signal_type: String = "",
    val signal_score: Int = 0,
    val market_bias: String = "",
    val reason_short: String = "",
    val reason_detail: String = "",
    val captured_at: String = "",
    val price: Double? = 0.0,
    val vwap: Double? = null,
    val board_over: Long? = null,
    val board_under: Long? = null,
    val best_bid_price: Double? = null,
    val best_bid_size: Long? = null,
    val best_ask_price: Double? = null,
    val best_ask_size: Long? = null,
    val change_value: Double? = 0.0,
    val change_rate: Double? = 0.0,
    val nikkei_change_rate: Double? = null,
    val volume: Double? = null,
    val source: String? = null,
    val market: String? = null,
    val price_time: String? = null
)
