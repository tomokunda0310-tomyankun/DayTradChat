//app/src/main/java/com/daytradchat/papa/model/ServerMessages.kt
//ver 2.16-22
package com.daytradchat.papa.model

data class DisclosureFlags(
    val today_disclosure_flag: Boolean? = null,
    val next_business_disclosure_flag: Boolean? = null
)

data class MarketItem(
    val type: String? = null,
    val code: String? = null,
    val name: String? = null,
    val side: String? = null,
    val market: String? = null,
    val captured_at: String? = null,
    val price: Double? = null,
    val change: Double? = null,
    val change_value: Double? = null,
    val change_rate: Double? = null,
    val prev_close: Double? = null,
    val score: Double? = null,
    val score_raw: Double? = null,
    val is_index: Boolean? = null,
    val category_tags: List<String>? = null,
    val price_time: String? = null,
    val source: String? = null
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
    val change: Double? = null,
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
    val best_bid_qty: Double? = null,
    val best_ask_price: Double? = null,
    val best_ask_size: Double? = null,
    val best_ask_qty: Double? = null,
    val nikkei_change_rate: Double? = null,
    val volume: Double? = null,
    val source: String? = null,
    val data: SignalData? = null,
    val side: String? = null,
    val score: Double? = null,
    val score_raw: Double? = null,
    val tick: Double? = null,
    val gap: Double? = null,
    val pts_ratio: Double? = null,
    val over: Double? = null,
    val under: Double? = null,
    val margin_sell: Double? = null,
    val margin_buy: Double? = null,
    val calendar_snippet: String? = null,
    val disclosure_flags: DisclosureFlags? = null,
    val is_client_requested: Boolean? = null,
    val is_index: Boolean? = null,
    val is_earnings: Boolean? = null,
    val earnings_type: String? = null,
    val category_tags: List<String>? = null,
    val trade_date: String? = null,
    val priority_score: Double? = null,
    val open_price: Double? = null,
    val high_price: Double? = null,
    val low_price: Double? = null
)

sealed class ServerMessage {
    data class ServerHello(
        val type: String? = null,
        val server_time: String? = null,
        val message: String? = null,
        val port: Int? = null
    ) : ServerMessage()

    data class Pong(
        val type: String? = null,
        val server_time: String? = null
    ) : ServerMessage()

    data class AckMessage(
        val type: String? = null,
        val message: String? = null,
        val original_type: String? = null,
        val client_name: String? = null,
        val client_version: String? = null
    ) : ServerMessage()

    data class ErrorMessage(
        val type: String? = null,
        val message: String? = null,
        val raw: String? = null,
        val detail: String? = null
    ) : ServerMessage()

    data class MasterMessage(
        val type: String? = null,
        val sent_at: String? = null,
        val count: Int? = null,
        val symbols: List<SignalItem>? = null
    ) : ServerMessage()

    data class WatchUpdateAckMessage(
        val type: String? = null,
        val mode: String? = null,
        val request: String? = null,
        val count: Int? = null,
        val symbols: List<String>? = null
    ) : ServerMessage()

    data class SignalBatch(
        val type: String? = null,
        val sent_at: String? = null,
        val long_count: Int? = null,
        val short_count: Int? = null,
        val count: Int? = null,
        val market: MarketItem? = null,
        val short_codes: List<String>? = null,
        val items: List<SignalItem>? = null,
        val symbols: List<SignalItem>? = null
    ) : ServerMessage()

    data class SignalSymbolMessage(
        val type: String? = null,
        val sent_at: String? = null,
        val symbol: SignalItem? = null
    ) : ServerMessage()

    data class IndexSnapshot(
        val type: String? = null,
        val sent_at: String? = null,
        val index: MarketItem? = null
    ) : ServerMessage()

    data class GetNowResponse(
        val type: String? = null,
        val sent_at: String? = null,
        val index: MarketItem? = null,
        val symbols: List<SignalItem>? = null
    ) : ServerMessage()
}
