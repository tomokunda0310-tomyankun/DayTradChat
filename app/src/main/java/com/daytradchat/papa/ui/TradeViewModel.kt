//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.16-26
package com.daytradchat.papa.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.daytradchat.papa.R
import com.daytradchat.papa.model.LogLineUiModel
import com.daytradchat.papa.model.MarketItem
import com.daytradchat.papa.model.ServerMessage
import com.daytradchat.papa.model.SignalCardUiModel
import com.daytradchat.papa.model.SignalItem
import com.daytradchat.papa.network.ConfigStore
import com.daytradchat.papa.network.ServerMessageParser
import com.daytradchat.papa.network.SocketClient
import com.daytradchat.papa.network.SocketConfig
import com.daytradchat.papa.network.SystemLogStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

private data class HoldingInfo(val shares: Int, val buyPrice: Double)

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = ServerMessageParser()
    private val configStore = ConfigStore(application)
    private val systemLogStore = SystemLogStore(application)
    private val prefs = application.getSharedPreferences("daytrade_client_state", Context.MODE_PRIVATE)
    private val displayDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)
    private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.JAPAN)

    private val signalMap = linkedMapOf<String, SignalItem>()
    private val historyMap = linkedMapOf<String, MutableList<String>>()
    private val holdings = linkedMapOf<String, HoldingInfo>()
    private val previousPriceMap = mutableMapOf<String, Double>()
    private val startPriceMap = mutableMapOf<String, Double>()
    private var marketItem: MarketItem? = null

    private val _statusLeft = MutableStateFlow("未接続")
    val statusLeft: StateFlow<String> = _statusLeft.asStateFlow()

    private val _statusRight = MutableStateFlow("")
    val statusRight: StateFlow<String> = _statusRight.asStateFlow()

    private val _hostLine = MutableStateFlow("")
    val hostLine: StateFlow<String> = _hostLine.asStateFlow()

    private val _signalItems = MutableStateFlow(createEmptySlots())
    val signalItems: StateFlow<List<SignalCardUiModel>> = _signalItems.asStateFlow()

    private val _logItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    val logItems: StateFlow<List<LogLineUiModel>> = _logItems.asStateFlow()

    private val _systemLogItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    val systemLogItems: StateFlow<List<LogLineUiModel>> = _systemLogItems.asStateFlow()

    private val _currentHost = MutableStateFlow(configStore.loadHost())
    val currentHost: StateFlow<String> = _currentHost.asStateFlow()

    private val _reconnectSec = MutableStateFlow(
        configStore.loadReconnectSec().coerceIn(1, 10).let { if (it == 0) 1 else it }
    )
    val reconnectSec: StateFlow<Int> = _reconnectSec.asStateFlow()

    private val _availableCodes = MutableStateFlow<List<String>>(emptyList())
    val availableCodes: StateFlow<List<String>> = _availableCodes.asStateFlow()

    private val _selectedDisplayCodes = MutableStateFlow(loadDisplayCodes())
    val selectedDisplayLabels: StateFlow<List<String>> = _selectedDisplayCodes.asStateFlow()

    private val socketClient = SocketClient(
        hostProvider = { _currentHost.value },
        reconnectDelayMsProvider = { _reconnectSec.value.toLong() * 1000L },
        onLineReceived = { line ->
            appendSystemLog("RECV: $line")
            try {
                handleIncomingLine(line)
            } catch (e: Exception) {
                appendSystemLog("HANDLE_LINE_ERROR: ${e.message ?: "unknown"}")
            }
        },
        onStatusChanged = { status ->
            _statusLeft.value = status
            _hostLine.value = hostLineText()
            if (status == "接続済") {
                _statusRight.value = displayDateFormat.format(Date())
            }
        },
        onSystemLog = { log -> appendSystemLog(log) }
    )

    init {
        rotateDayIfNeeded()
        loadHoldings()
        _hostLine.value = hostLineText()
        _systemLogItems.value = systemLogStore.loadToday().reversed().map {
            LogLineUiModel(UUID.randomUUID().toString(), it)
        }
    }

    fun startSocket() = socketClient.start()
    fun stopSocket() = socketClient.stopAsync()

    fun saveSettingsAndReconnect(host: String, reconnectSecText: String) {
        val sec = reconnectSecText.toIntOrNull()?.coerceIn(1, 10) ?: 1
        configStore.saveHost(host)
        configStore.saveReconnectSec(sec)
        _currentHost.value = configStore.loadHost()
        _reconnectSec.value = configStore.loadReconnectSec().coerceIn(1, 10)
        _hostLine.value = hostLineText()
        socketClient.restart()
    }

    fun resetSettingsAndReconnect() {
        configStore.resetAll()
        _currentHost.value = configStore.loadHost()
        _reconnectSec.value = configStore.loadReconnectSec().coerceIn(1, 10).let { if (it == 0) 1 else it }
        _hostLine.value = hostLineText()
        socketClient.restart()
    }

    fun sendWatchCommand(targetLabel: String, inputText: String) {
        val directCode = extractCode(targetLabel)
        val parsed = inputText
            .split(Regex("[,\\s\\n\\r\\t、]+"))
            .map { normalizeCode(it) }
            .filter { it.isNotBlank() }
            .toMutableList()
        if (parsed.isEmpty() && directCode.isNotBlank()) {
            parsed += directCode
        }
        if (parsed.isEmpty()) return
        socketClient.sendAddCodes(parsed)
        appendUserLog("ADD_CODES ${parsed.joinToString(",")}")
    }

    fun applyHolding(codeLabel: String, shares: Int, buyPriceText: String) {
        val code = extractCode(codeLabel)
        val buyPrice = buyPriceText.toDoubleOrNull() ?: return
        if (code.isBlank() || shares <= 0) return
        holdings[code] = HoldingInfo(shares, buyPrice)
        saveHoldings()
        rebuildSignalItems()
    }

    fun addDisplayCode(label: String) {
        val code = extractCode(label)
        if (code.isBlank()) return
        val current = _selectedDisplayCodes.value.toMutableList()
        if (current.contains(code)) return
        if (current.size >= 8) return
        current.add(code)
        _selectedDisplayCodes.value = current
        saveDisplayCodes(current)
        rebuildSignalItems()
    }

    fun removeDisplayCodeAt(position: Int) {
        val current = _selectedDisplayCodes.value.toMutableList()
        if (position !in current.indices) return
        current.removeAt(position)
        _selectedDisplayCodes.value = current
        saveDisplayCodes(current)
        rebuildSignalItems()
    }

    fun resetDisplayCodesToday() {
        _selectedDisplayCodes.value = autoSelectedCodes()
        saveDisplayCodes(_selectedDisplayCodes.value)
        rebuildSignalItems()
    }

    fun getProfitDisplay(code: String, currentPrice: Double): ProfitDisplay {
        val holding = holdings[code] ?: return ProfitDisplay("")
        val profit = ((currentPrice - holding.buyPrice) * holding.shares).roundToLong().toInt()
        return when {
            profit > 0 -> ProfitDisplay("損益 +$profit  ${holding.shares}株", true)
            profit < 0 -> ProfitDisplay("損益 $profit  ${holding.shares}株", false)
            else -> ProfitDisplay("損益 0  ${holding.shares}株", null)
        }
    }

    fun getPriceVisual(code: String, currentPrice: Double): PriceVisual {
        val prev = previousPriceMap[code]
        val start = startPriceMap[code] ?: currentPrice

        val bgColor = when {
            prev == null -> R.color.skip_bg
            currentPrice > prev -> R.color.sell_bg
            currentPrice < prev -> R.color.buy_bg
            else -> R.color.skip_bg
        }

        val codeNameColor = if (currentPrice > start) R.color.profit_plus else R.color.profit_minus
        return PriceVisual(bgColorRes = bgColor, codeNameColorRes = codeNameColor)
    }

    fun buildHistoryDialogText(code: String): String {
        val rows = historyMap[code].orEmpty()
        return if (rows.isEmpty()) "履歴なし" else rows.joinToString("\n")
    }

    private fun handleIncomingLine(line: String) {
        val message = parser.parse(line)
        when (message) {
            is ServerMessage.ServerHello -> {
                _statusLeft.value = "接続済"
                _statusRight.value = (message.server_time ?: "").replace("-", "/")
                appendUserLog("HELLO ${message.server_time.orEmpty()}")
            }
            is ServerMessage.Pong -> appendUserLog("PONG ${message.server_time.orEmpty()}")
            is ServerMessage.AckMessage -> appendUserLog("ACK ${(message.original_type ?: "register").ifBlank { "register" }}")
            is ServerMessage.ErrorMessage -> appendUserLog("ERROR ${message.message.orEmpty()}")
            is ServerMessage.MasterMessage -> {
                val symbols = message.symbols.orEmpty()
                symbols.forEach { item ->
                    val code = extractItemCode(item)
                    if (code.isNotBlank()) {
                        signalMap[code] = signalMap[code]?.mergeWith(item) ?: item
                    }
                }
                if (_selectedDisplayCodes.value.isEmpty()) {
                    _selectedDisplayCodes.value = autoSelectedCodes()
                    saveDisplayCodes(_selectedDisplayCodes.value)
                }
                updateAvailableCodes()
                rebuildSignalItems()
            }
            is ServerMessage.IndexSnapshot -> {
                marketItem = message.index
                rebuildSignalItems()
            }
            is ServerMessage.GetNowResponse -> {
                marketItem = message.index ?: marketItem
                message.symbols.orEmpty().forEach { item ->
                    val code = extractItemCode(item)
                    if (code.isNotBlank()) {
                        signalMap[code] = signalMap[code]?.mergeWith(item) ?: item
                    }
                }
                if (_selectedDisplayCodes.value.isEmpty()) {
                    _selectedDisplayCodes.value = autoSelectedCodes()
                    saveDisplayCodes(_selectedDisplayCodes.value)
                }
                updateAvailableCodes()
                rebuildSignalItems()
            }
            is ServerMessage.SignalSymbolMessage -> {
                val item = message.symbol
                if (item != null) {
                    val code = extractItemCode(item)
                    if (code.isNotBlank()) {
                        val merged = signalMap[code]?.mergeWith(item) ?: item
                        val price = merged.price ?: merged.data?.price ?: 0.0
                        if (!startPriceMap.containsKey(code)) {
                            startPriceMap[code] = price
                        }
                        val oldPrice = signalMap[code]?.price ?: signalMap[code]?.data?.price ?: price
                        previousPriceMap[code] = oldPrice
                        signalMap[code] = merged
                        if (_selectedDisplayCodes.value.isEmpty()) {
                            _selectedDisplayCodes.value = autoSelectedCodes()
                            saveDisplayCodes(_selectedDisplayCodes.value)
                        }
                        updateAvailableCodes()
                        rebuildSignalItems()
                    }
                }
            }
            is ServerMessage.SignalBatch -> {
                marketItem = message.market ?: marketItem
                val symbols = when {
                    !message.symbols.isNullOrEmpty() -> message.symbols
                    !message.items.isNullOrEmpty() -> message.items
                    else -> emptyList()
                }
                symbols.forEach { item ->
                    val code = extractItemCode(item)
                    if (code.isNotBlank()) {
                        val merged = signalMap[code]?.mergeWith(item) ?: item
                        val price = merged.price ?: merged.data?.price ?: 0.0
                        if (!startPriceMap.containsKey(code)) {
                            startPriceMap[code] = price
                        }
                        val oldPrice = signalMap[code]?.price ?: signalMap[code]?.data?.price ?: price
                        previousPriceMap[code] = oldPrice
                        signalMap[code] = merged
                    }
                }
                if (_selectedDisplayCodes.value.isEmpty()) {
                    _selectedDisplayCodes.value = autoSelectedCodes()
                    saveDisplayCodes(_selectedDisplayCodes.value)
                }
                updateAvailableCodes()
                rebuildSignalItems()
            }
            else -> appendUserLog(line)
        }
    }

    private fun rebuildSignalItems() {
        rotateDayIfNeeded()
        val list = mutableListOf<SignalCardUiModel>()
        list += buildIndexCard()
        _selectedDisplayCodes.value.take(17).forEachIndexed { index, code ->
            list += buildSignalCard("slot_${index + 1}", code)
        }
        while (list.size < 18) list += emptyStock("slot_${list.size}")
        _signalItems.value = list.take(18)
    }

    private fun buildIndexCard(): SignalCardUiModel {
        val m = marketItem
        return if (m == null) {
            SignalCardUiModel(
                slotId = "slot_0",
                code = "NIKKEI225",
                name = "日経平均",
                signalType = "INDEX",
                score = 0,
                price = 0.0,
                changeRate = 0.0,
                reasonShort = "待機中",
                updatedAt = "",
                isEmpty = false
            )
        } else {
            val idxCode = m.code ?: "NIKKEI225"
            val idxPrice = m.price ?: 0.0
            if (!startPriceMap.containsKey(idxCode)) {
                startPriceMap[idxCode] = idxPrice
            }
            previousPriceMap[idxCode] = previousPriceMap[idxCode] ?: idxPrice
            SignalCardUiModel(
                slotId = "slot_0",
                code = idxCode,
                name = m.name ?: "日経平均",
                signalType = "INDEX",
                score = 0,
                price = idxPrice,
                changeRate = m.change_rate ?: 0.0,
                reasonShort = "日経平均",
                updatedAt = shortTime(m.captured_at ?: ""),
                isEmpty = false
            )
        }
    }

    private fun buildSignalCard(slotId: String, code: String): SignalCardUiModel {
        val item = signalMap[code] ?: return emptyStock(slotId, code)
        val side = (item.side ?: item.signal_type ?: "SKIP").uppercase(Locale.ROOT)
        val signalType = when (side) {
            "LONG", "BUY" -> "BUY"
            "SHORT", "SELL" -> "SELL"
            else -> "SKIP"
        }
        val price = item.price ?: item.data?.price ?: 0.0
        val name = item.name ?: item.data?.name ?: "名称未取得"
        val score = item.score?.toInt() ?: item.score_raw?.toInt() ?: item.signal_score ?: item.data?.signal_score ?: 0
        val reason = when (signalType) {
            "BUY" -> "ロング候補"
            "SELL" -> "ショート候補"
            else -> item.reason_short ?: item.data?.reason_short ?: "候補"
        }
        val updatedAt = shortTime(item.sent_at ?: item.captured_at ?: item.data?.captured_at ?: "")
        rememberHistory(code, price, updatedAt, reason, score)
        return SignalCardUiModel(
            slotId = slotId,
            code = code,
            name = name,
            signalType = signalType,
            score = score,
            price = price,
            changeRate = item.change_rate ?: item.data?.change_rate ?: 0.0,
            reasonShort = reason,
            updatedAt = updatedAt,
            isEmpty = false
        )
    }

    private fun updateAvailableCodes() {
        _availableCodes.value = signalMap.values
            .mapNotNull { item ->
                val code = extractItemCode(item)
                val name = item.name ?: item.data?.name ?: ""
                if (code.isBlank()) null else code to shortName(name)
            }
            .sortedBy { it.second }
            .map { "${it.first} ${it.second}" }
    }

    private fun autoSelectedCodes(): List<String> {
        return signalMap.values
            .mapNotNull { item ->
                val code = extractItemCode(item)
                val name = item.name ?: item.data?.name ?: ""
                if (code.isBlank()) null else code to name
            }
            .sortedBy { it.second }
            .take(17)
            .map { it.first }
    }

    private fun shortName(name: String): String = if (name.length <= 10) name else name.take(10) + "…"
    private fun extractItemCode(item: SignalItem): String = normalizeCode(item.code ?: item.data?.code ?: "")
    private fun extractCode(label: String): String = normalizeCode(label.substringBefore(" ").trim())
    private fun normalizeCode(raw: String): String = raw.trim().uppercase(Locale.ROOT)

    private fun SignalItem.mergeWith(newer: SignalItem): SignalItem {
        return copy(
            type = newer.type ?: type,
            sent_at = newer.sent_at ?: sent_at,
            title = newer.title ?: title,
            body = newer.body ?: body,
            code = newer.code ?: code,
            name = newer.name ?: name,
            market = newer.market ?: market,
            captured_at = newer.captured_at ?: captured_at,
            price = newer.price ?: price,
            change = newer.change ?: change,
            change_value = newer.change_value ?: change_value,
            change_rate = newer.change_rate ?: change_rate,
            prev_close = newer.prev_close ?: prev_close,
            price_time = newer.price_time ?: price_time,
            signal_type = newer.signal_type ?: signal_type,
            signal_score = newer.signal_score ?: signal_score,
            market_bias = newer.market_bias ?: market_bias,
            reason_short = newer.reason_short ?: reason_short,
            reason_detail = newer.reason_detail ?: reason_detail,
            vwap = newer.vwap ?: vwap,
            board_over = newer.board_over ?: board_over,
            board_under = newer.board_under ?: board_under,
            best_bid_price = newer.best_bid_price ?: best_bid_price,
            best_bid_size = newer.best_bid_size ?: best_bid_size,
            best_bid_qty = newer.best_bid_qty ?: best_bid_qty,
            best_ask_price = newer.best_ask_price ?: best_ask_price,
            best_ask_size = newer.best_ask_size ?: best_ask_size,
            best_ask_qty = newer.best_ask_qty ?: best_ask_qty,
            nikkei_change_rate = newer.nikkei_change_rate ?: nikkei_change_rate,
            volume = newer.volume ?: volume,
            source = newer.source ?: source,
            data = newer.data ?: data,
            side = newer.side ?: side,
            score = newer.score ?: score,
            score_raw = newer.score_raw ?: score_raw,
            tick = newer.tick ?: tick,
            gap = newer.gap ?: gap,
            pts_ratio = newer.pts_ratio ?: pts_ratio,
            over = newer.over ?: over,
            under = newer.under ?: under,
            margin_sell = newer.margin_sell ?: margin_sell,
            margin_buy = newer.margin_buy ?: margin_buy,
            calendar_snippet = newer.calendar_snippet ?: calendar_snippet,
            disclosure_flags = newer.disclosure_flags ?: disclosure_flags,
            is_client_requested = newer.is_client_requested ?: is_client_requested,
            is_index = newer.is_index ?: is_index,
            is_earnings = newer.is_earnings ?: is_earnings,
            earnings_type = newer.earnings_type ?: earnings_type,
            category_tags = newer.category_tags ?: category_tags,
            trade_date = newer.trade_date ?: trade_date,
            priority_score = newer.priority_score ?: priority_score,
            open_price = newer.open_price ?: open_price,
            high_price = newer.high_price ?: high_price,
            low_price = newer.low_price ?: low_price
        )
    }

    private fun rememberHistory(code: String, price: Double, updatedAt: String, reason: String, score: Int) {
        val list = historyMap.getOrPut(code) { mutableListOf() }
        list.add("${updatedAt.padEnd(8)}  ${formatCompact(price).padStart(8)}  score ${score}  $reason")
        while (list.size > 30) list.removeAt(0)
    }

    private fun loadDisplayCodes(): List<String> {
        rotateDayIfNeeded()
        return prefs.getString("display_codes", "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun saveDisplayCodes(codes: List<String>) {
        prefs.edit().putString("display_codes", codes.joinToString(",")).apply()
        prefs.edit().putString("display_codes_day", dayKeyFormat.format(Date())).apply()
    }

    private fun rotateDayIfNeeded() {
        val today = dayKeyFormat.format(Date())
        val saved = prefs.getString("display_codes_day", "") ?: ""
        if (saved != today) {
            prefs.edit().remove("display_codes").putString("display_codes_day", today).apply()
        }
    }

    private fun loadHoldings() {
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("holding_")) {
                val code = k.removePrefix("holding_")
                val raw = v?.toString().orEmpty()
                val parts = raw.split("|")
                if (parts.size == 2) {
                    val shares = parts[0].toIntOrNull() ?: 0
                    val buyPrice = parts[1].toDoubleOrNull() ?: 0.0
                    if (shares > 0 && buyPrice > 0.0) {
                        holdings[code] = HoldingInfo(shares, buyPrice)
                    }
                }
            }
        }
    }

    private fun saveHoldings() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("holding_") }.forEach { editor.remove(it) }
        holdings.forEach { (code, info) ->
            editor.putString("holding_$code", "${info.shares}|${info.buyPrice}")
        }
        editor.apply()
    }

    private fun hostLineText(): String {
        val host = _currentHost.value.trim().ifBlank { "未設定" }
        return "host: $host  port: ${SocketConfig.SERVER_PORT}  reconnect: ${_reconnectSec.value}s"
    }

    private fun shortTime(value: String): String = if (value.length >= 8) value.takeLast(8) else value

    private fun appendUserLog(text: String) {
        _logItems.value = (listOf(LogLineUiModel(UUID.randomUUID().toString(), text)) + _logItems.value).take(80)
    }

    private fun appendSystemLog(text: String) {
        systemLogStore.append(text)
        _systemLogItems.value = (listOf(LogLineUiModel(UUID.randomUUID().toString(), text)) + _systemLogItems.value).take(500)
    }

    private fun createEmptySlots(): List<SignalCardUiModel> {
        val list = mutableListOf<SignalCardUiModel>()
        list += buildIndexCard()
        for (i in 1 until 18) list += emptyStock("slot_$i")
        return list
    }

    private fun emptyStock(slotId: String, code: String = "--"): SignalCardUiModel {
        return SignalCardUiModel(
            slotId = slotId,
            code = code,
            name = "待機中",
            signalType = "SKIP",
            score = 0,
            price = 0.0,
            changeRate = 0.0,
            reasonShort = "待機中",
            updatedAt = "",
            isEmpty = false
        )
    }

    private fun formatCompact(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
    }

    override fun onCleared() {
        socketClient.stopAsync()
        super.onCleared()
    }
}
