//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.15-20
package com.daytradchat.papa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.daytradchat.papa.model.LogLineUiModel
import com.daytradchat.papa.model.MarketItem
import com.daytradchat.papa.model.ServerMessage
import com.daytradchat.papa.model.SignalCardUiModel
import com.daytradchat.papa.model.SignalHistoryUiModel
import com.daytradchat.papa.model.SignalItem
import com.daytradchat.papa.network.ConfigStore
import com.daytradchat.papa.network.HoldingPref
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

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = ServerMessageParser()
    private val configStore = ConfigStore(application)
    private val systemLogStore = SystemLogStore(application)
    private val historyMap = linkedMapOf<String, MutableList<SignalHistoryUiModel>>()
    private val watchedCodes = mutableListOf<String>()
    private val signalMap = linkedMapOf<String, SignalCardUiModel>()
    private var marketCard: SignalCardUiModel? = null
    private var holdings: Map<String, HoldingPref> = configStore.loadHoldings()
    private val displayDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)

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

    private val _reconnectSec = MutableStateFlow(configStore.loadReconnectSec())
    val reconnectSec: StateFlow<Int> = _reconnectSec.asStateFlow()

    private val _availableCodes = MutableStateFlow<List<String>>(emptyList())
    val availableCodes: StateFlow<List<String>> = _availableCodes.asStateFlow()

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
        onSystemLog = { appendSystemLog(it) }
    )

    init {
        _hostLine.value = hostLineText()
        _systemLogItems.value = systemLogStore.loadToday().reversed().map {
            LogLineUiModel(UUID.randomUUID().toString(), it)
        }
        publishAvailableCodes()
    }

    fun startSocket() = socketClient.start()
    fun stopSocket() = socketClient.stopAsync()

    fun saveSettingsAndReconnect(host: String, reconnectSecText: String) {
        val sec = reconnectSecText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        configStore.saveHost(host)
        configStore.saveReconnectSec(sec)
        _currentHost.value = configStore.loadHost()
        _reconnectSec.value = configStore.loadReconnectSec()
        _hostLine.value = hostLineText()
        appendSystemLog("CONFIG_SAVE: host=${_currentHost.value}, reconnectSec=${_reconnectSec.value}")
        socketClient.restart()
    }

    fun resetSettingsAndReconnect() {
        configStore.resetAll()
        holdings = emptyMap()
        _currentHost.value = configStore.loadHost()
        _reconnectSec.value = configStore.loadReconnectSec()
        _hostLine.value = hostLineText()
        appendSystemLog("CONFIG_RESET")
        renderCards()
        socketClient.restart()
    }

    fun applyHolding(code: String, quantity: Int, buyPriceText: String) {
        val buyPrice = buyPriceText.toDoubleOrNull() ?: return
        if (code.isBlank() || quantity <= 0) return
        configStore.saveHolding(code, quantity, buyPrice)
        holdings = configStore.loadHoldings()
        appendUserLog("HOLD $code qty=$quantity buy=$buyPrice")
        renderCards()
    }

    fun sendWatchCommand(targetCode: String, inputText: String) {
        val raw = inputText.trim()
        if (raw.isBlank()) return
        val sendText = when {
            raw.startsWith("{") -> raw
            raw.contains(",") || raw.contains("#") -> raw
            targetCode.isNotBlank() -> "$targetCode#$raw"
            else -> raw
        }
        socketClient.sendRawLine(sendText)
    }

    fun buildHistoryDialogText(code: String): String {
        val rows = historyMap[code].orEmpty()
        if (rows.isEmpty()) return "履歴なし"
        val lines = mutableListOf<String>()
        lines += "価格推移"
        lines += buildSparkline(rows.map { it.price })
        lines += ""
        lines += "時刻        価格      前日比   score  内容"
        rows.asReversed().take(10).forEach { row ->
            lines += "${row.time.padEnd(8)}  ${formatCompact(row.price).padStart(8)}  ${(formatSigned(row.changeRate) + "%").padStart(7)}  ${row.score.toString().padStart(5)}  ${row.reasonShort}"
        }
        return lines.joinToString("\n")
    }

    private fun handleIncomingLine(line: String) {
        val message = parser.parse(line)
        if (message == null) {
            appendUserLog(line)
            return
        }
        when (message) {
            is ServerMessage.ServerHello -> {
                _statusLeft.value = "接続済"
                _statusRight.value = formatServerTime(message.server_time)
                appendUserLog("HELLO ${message.server_time.orEmpty()}")
            }
            is ServerMessage.Pong -> appendUserLog("PONG ${message.server_time.orEmpty()}")
            is ServerMessage.AckMessage -> {
                val typeLabel = message.original_type?.takeIf { it.isNotBlank() } ?: (message.type ?: "ack")
                appendUserLog("ACK $typeLabel: ${message.message.orEmpty()}")
            }
            is ServerMessage.ErrorMessage -> {
                val compact = buildString {
                    append("ERROR ")
                    append(message.message.orEmpty())
                    if (!message.detail.isNullOrBlank()) {
                        append(" / ")
                        append(message.detail)
                    }
                }
                appendUserLog(compact)
            }
            is ServerMessage.MasterMessage -> {
                watchedCodes.clear()
                message.symbols.orEmpty().forEach { item ->
                    val code = item.code.orEmpty().trim()
                    if (code.isNotBlank()) watchedCodes += code
                    val ui = itemToUi("slot_x", item)
                    signalMap[ui.code] = ui
                }
                appendUserLog("MASTER count=${message.count ?: message.symbols.orEmpty().size}")
                renderCards()
            }
            is ServerMessage.WatchUpdateAckMessage -> {
                watchedCodes.clear()
                watchedCodes += message.symbols.orEmpty().filter { it.isNotBlank() }
                appendUserLog("WATCH ${message.mode.orEmpty()} ${message.request.orEmpty()}")
                renderCards()
            }
            is ServerMessage.SignalBatch -> {
                val batchSymbols = when {
                    !message.symbols.isNullOrEmpty() -> message.symbols.orEmpty()
                    !message.items.isNullOrEmpty() -> message.items.orEmpty()
                    else -> emptyList()
                }
                if (message.market != null) marketCard = marketToUi("slot_0", message.market)
                if (watchedCodes.isEmpty()) {
                    watchedCodes.clear()
                    watchedCodes += batchSymbols.mapNotNull { it.code?.trim() }.filter { it.isNotBlank() }.take(8)
                }
                batchSymbols.forEach { item ->
                    val ui = itemToUi("slot_x", item)
                    signalMap[ui.code] = ui
                }
                appendUserLog("BATCH ${message.sent_at.orEmpty()} count=${batchSymbols.size}")
                renderCards()
            }
            is ServerMessage.SignalSymbolMessage -> {
                val symbol = message.symbol
                if (symbol != null) {
                    val ui = itemToUi("slot_x", symbol)
                    signalMap[ui.code] = ui
                    if (ui.code.isNotBlank() && watchedCodes.none { it == ui.code }) {
                        watchedCodes += ui.code
                        while (watchedCodes.size > 8) watchedCodes.removeAt(watchedCodes.lastIndex)
                    }
                    appendUserLog("SYMBOL ${ui.code} ${ui.name}")
                    renderCards()
                }
            }
        }
    }

    private fun renderCards() {
        val list = mutableListOf<SignalCardUiModel>()
        list += marketCard ?: emptyIndex("slot_0")
        val codes = if (watchedCodes.isNotEmpty()) watchedCodes.take(8) else signalMap.keys.take(8).toList()
        codes.forEachIndexed { idx, code ->
            val current = signalMap[code]
            list += if (current != null) applyHolding(current.copy(slotId = "slot_${idx + 1}")) else emptyStock("slot_${idx + 1}", code)
        }
        while (list.size < 9) list += emptyStock("slot_${list.size}", "--")
        _signalItems.value = list.take(9)
        recordHistory(_signalItems.value)
        publishAvailableCodes()
    }

    private fun publishAvailableCodes() {
        _availableCodes.value = _signalItems.value.filter { !it.isEmpty && it.code != "NIKKEI225" && it.code != "--" }.map { it.code }
    }

    private fun applyHolding(item: SignalCardUiModel): SignalCardUiModel {
        val h = holdings[item.code] ?: return item.copy(profitText = "")
        val profit = if (item.signalType == "SELL") {
            (h.buyPrice - item.price) * h.quantity
        } else {
            (item.price - h.buyPrice) * h.quantity
        }
        val text = "損益 ${formatSignedValue(profit)}  ${h.quantity}株 @${formatCompact(h.buyPrice)}"
        return item.copy(profitText = text)
    }

    private fun marketToUi(slotId: String, market: MarketItem): SignalCardUiModel {
        return SignalCardUiModel(
            slotId = slotId,
            code = market.code.orEmpty().ifBlank { "NIKKEI225" },
            name = market.name.orEmpty().ifBlank { "日経平均" },
            signalType = "INDEX",
            score = 0,
            price = market.price ?: 0.0,
            changeRate = market.change_rate ?: 0.0,
            reasonShort = "日経平均",
            updatedAt = shortTime(market.captured_at.orEmpty()),
            isEmpty = false
        )
    }

    private fun itemToUi(slotId: String, item: SignalItem): SignalCardUiModel {
        val d = item.data
        val code = firstNotBlank(d?.code, item.code) ?: "--"
        val name = firstNotBlank(d?.name, item.name, extractNameFromTitle(item.title, code, item.side, item.signal_type)) ?: "名称未取得"
        val side = firstNotBlank(d?.signal_type, item.signal_type, item.side)?.uppercase(Locale.ROOT).orEmpty()
        val signalType = when (side) {
            "LONG", "BUY" -> "BUY"
            "SHORT", "SELL" -> "SELL"
            else -> "SKIP"
        }
        val score = d?.signal_score ?: item.signal_score ?: item.score ?: 0
        val price = d?.price ?: item.price ?: 0.0
        val changeRate = d?.change_rate ?: item.change_rate ?: 0.0
        val reason = firstNotBlank(d?.reason_short, item.reason_short, deriveReason(item.side, item.body)) ?: "候補"
        val updated = shortTime(firstNotBlank(d?.captured_at, item.captured_at, item.sent_at, item.price_time) ?: "")
        return SignalCardUiModel(
            slotId = slotId,
            code = code,
            name = name,
            signalType = signalType,
            score = score,
            price = price,
            changeRate = changeRate,
            reasonShort = reason,
            updatedAt = updated,
            isEmpty = false
        )
    }

    private fun deriveReason(side: String?, body: String?): String {
        return when (side?.uppercase(Locale.ROOT).orEmpty()) {
            "LONG" -> "ロング候補"
            "SHORT" -> "ショート候補"
            else -> body.orEmpty().ifBlank { "候補" }
        }
    }

    private fun extractNameFromTitle(title: String?, code: String, side: String?, signalType: String?): String? {
        val raw = title.orEmpty().trim()
        if (raw.isBlank()) return null
        return raw
            .replace(code, "")
            .replace(side.orEmpty(), "", ignoreCase = true)
            .replace(signalType.orEmpty(), "", ignoreCase = true)
            .trim()
            .ifBlank { null }
    }

    private fun firstNotBlank(vararg values: String?): String? {
        for (v in values) {
            val t = v?.trim().orEmpty()
            if (t.isNotBlank()) return t
        }
        return null
    }

    private fun recordHistory(items: List<SignalCardUiModel>) {
        items.filter { !it.isEmpty && it.code != "--" }.forEach { item ->
            val list = historyMap.getOrPut(item.code) { mutableListOf() }
            list += SignalHistoryUiModel(
                time = item.updatedAt.ifBlank { shortTime(displayDateFormat.format(Date())) },
                price = item.price,
                changeRate = item.changeRate,
                score = item.score,
                reasonShort = item.reasonShort
            )
            while (list.size > 30) list.removeAt(0)
        }
    }

    private fun appendUserLog(text: String) {
        _logItems.value = (listOf(LogLineUiModel(UUID.randomUUID().toString(), text)) + _logItems.value).take(80)
    }

    private fun appendSystemLog(text: String) {
        systemLogStore.append(text)
        _systemLogItems.value = (listOf(LogLineUiModel(UUID.randomUUID().toString(), text)) + _systemLogItems.value).take(500)
    }

    private fun createEmptySlots(): List<SignalCardUiModel> {
        val list = mutableListOf<SignalCardUiModel>()
        list += emptyIndex("slot_0")
        for (i in 1 until 9) list += emptyStock("slot_$i", "--")
        return list
    }

    private fun emptyIndex(slotId: String) = SignalCardUiModel(
        slotId = slotId,
        code = "NIKKEI225",
        name = "日経平均",
        signalType = "INDEX",
        score = 0,
        price = 0.0,
        changeRate = 0.0,
        reasonShort = "待機中",
        updatedAt = "",
        isEmpty = true
    )

    private fun emptyStock(slotId: String, codeHint: String) = SignalCardUiModel(
        slotId = slotId,
        code = codeHint,
        name = if (codeHint == "--") "待機中" else "受信待ち",
        signalType = "SKIP",
        score = 0,
        price = 0.0,
        changeRate = 0.0,
        reasonShort = "データ待機",
        updatedAt = "",
        isEmpty = codeHint == "--"
    )

    private fun hostLineText(): String {
        val host = _currentHost.value.trim()
        return if (host.isBlank()) {
            "host: 未設定  port: ${SocketConfig.SERVER_PORT}  reconnect: ${_reconnectSec.value}s"
        } else {
            "host: $host  port: ${SocketConfig.SERVER_PORT}  reconnect: ${_reconnectSec.value}s"
        }
    }

    private fun formatServerTime(value: String?): String = value.orEmpty().replace("-", "/")

    private fun shortTime(value: String): String {
        return when {
            value.length >= 8 -> value.takeLast(8)
            else -> value
        }
    }

    private fun buildSparkline(values: List<Double>): String {
        if (values.isEmpty()) return "-"
        val chars = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        if (min == max) return List(values.size) { "▄" }.joinToString("")
        return values.joinToString("") { value ->
            val ratio = (value - min) / (max - min)
            val index = (ratio * (chars.size - 1)).roundToLong().toInt().coerceIn(0, chars.size - 1)
            chars[index]
        }
    }

    private fun formatCompact(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
    }

    private fun formatSigned(v: Double): String {
        return when {
            v > 0.0 -> "+%.1f".format(v)
            v < 0.0 -> "%.1f".format(v)
            else -> "0.0"
        }
    }

    private fun formatSignedValue(v: Double): String {
        return when {
            v > 0.0 -> "+${formatCompact(v)}"
            v < 0.0 -> formatCompact(v)
            else -> "0"
        }
    }

    override fun onCleared() {
        socketClient.stopAsync()
        super.onCleared()
    }
}
