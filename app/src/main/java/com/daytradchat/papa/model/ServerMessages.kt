//app/src/main/java/com/daytradchat/papa/model/ServerMessage.kt
//ver 2.13-14

package com.daytradchat.papa.model

sealed class ServerMessage {

    data class ServerHello(
        val type: String = "",
        val server_time: String? = null,
        val message: String? = null,
        val port: Int? = null
    ) : ServerMessage()

    data class Pong(
        val type: String = "",
        val server_time: String? = null
    ) : ServerMessage()

    data class AckMessage(
        val type: String = "",
        val message: String? = null,
        val original_type: String? = null,
        val client_name: String? = null,
        val client_version: String? = null
    ) : ServerMessage()

    data class ErrorMessage(
        val type: String = "",
        val message: String? = null,
        val raw: String? = null,
        val detail: String? = null
    ) : ServerMessage()

    data class SignalBatch(
        val type: String = "",
        val sent_at: String? = null,
        val market: MarketData? = null,
        val short_codes: List<String>? = null,
        val count: Int? = null,
        val items: List<SignalItem>? = null
    ) : ServerMessage()

    data class MarketData(
        val type: String? = null,
        val code: String? = null,
        val name: String? = null,
        val market: String? = null,
        val captured_at: String? = null,
        val price: Double? = null,
        val change_value: Double? = null,
        val change_rate: Double? = null,
        val prev_close: Double? = null,
        val price_time: String? = null,
        val source: String? = null
    )

    data class SignalItem(
        val type: String? = null,
        val sent_at: String? = null,
        val title: String? = null,
        val body: String? = null,
        val code: String? = null,
        val name: String? = null,
        val market: String? = null,
        val captured_at: String? = null,
        val price: Double? = null,
        val change_value: Double? = null,
        val change_rate: Double? = null,
        val prev_close: Double? = null,
        val price_time: String? = null,
        val signal_type: String? = null,
        val signal_score: Int? = null,
        val market_bias: String? = null,
        val reason_short: String? = null,
        val reason_detail: String? = null,
        val vwap: Double? = null,
        val board_over: Double? = null,
        val board_under: Double? = null,
        val best_bid_price: Double? = null,
        val best_bid_size: Double? = null,
        val best_ask_price: Double? = null,
        val best_ask_size: Double? = null,
        val nikkei_change_rate: Double? = null,
        val volume: Double? = null,
        val source: String? = null,
        val data: SignalData? = null
    )

    data class SignalData(
        val code: String? = null,
        val name: String? = null,
        val market: String? = null,
        val captured_at: String? = null,
        val price: Double? = null,
        val change_value: Double? = null,
        val change_rate: Double? = null,
        val prev_close: Double? = null,
        val price_time: String? = null,
        val signal_type: String? = null,
        val signal_score: Int? = null,
        val market_bias: String? = null,
        val reason_short: String? = null,
        val reason_detail: String? = null,
        val vwap: Double? = null,
        val board_over: Double? = null,
        val board_under: Double? = null,
        val best_bid_price: Double? = null,
        val best_bid_size: Double? = null,
        val best_ask_price: Double? = null,
        val best_ask_size: Double? = null,
        val nikkei_change_rate: Double? = null,
        val volume: Double? = null,
        val source: String? = null
    )
}
