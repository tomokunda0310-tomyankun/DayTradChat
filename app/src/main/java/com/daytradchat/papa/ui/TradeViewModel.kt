//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.16-15
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

private data class HoldingInfo(val buyPrice: Double)

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
    private val samePriceCountMap = mutableMapOf<String, Int>()
    private val lastTrendMap = mutableMapOf<String, Int>()
    private val hiddenDisplayCodes = linkedSetOf<String>()
    private var lastRemovedDisplayCode: String? = null
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

    private val _selectedDisplayCodes = MutableStateFlow<List<String>>(emptyList())
    private val _selectedDisplayLabels = MutableStateFlow<List<String>>(emptyList())
    val selectedDisplayLabels: StateFlow<List<String>> = _selectedDisplayLabels.asStateFlow()

    private val _canUndoDisplayRemoval = MutableStateFlow(false)
    val canUndoDisplayRemoval: StateFlow<Boolean> = _canUndoDisplayRemoval.asStateFlow()

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
        loadHiddenDisplayCodes()
        loadHoldings()
        _hostLine.value = hostLineText()
        _systemLogItems.value = systemLogStore.loadToday().reversed().map {
            LogLineUiModel(UUID.randomUUID().toString(), it)
        }
        refreshDisplaySelection()
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

    fun sendWatchCommand(inputCode: String) {
        val payload = inputCode.trim()
        if (payload.isBlank()) return
        socketClient.sendRawLine(payload)
    }

    fun applyHolding(codeLabel: String, buyPriceText: String) {
        val code = extractCode(codeLabel)
        if (code.isBlank()) return
        val buyPrice = buyPriceText.toDoubleOrNull() ?: return
        if (buyPrice <= 0.0) {
            holdings.remove(code)
        } else {
            holdings[code] = HoldingInfo(buyPrice = buyPrice)
        }
        saveHoldings()
        rebuildSignalItems()
    }

    fun removeDisplayCodeAt(position: Int) {
        val current = _selectedDisplayCodes.value
        if (position !in current.indices) return
        val code = current[position]
        hiddenDisplayCodes.add(code)
        lastRemovedDisplayCode = code
        saveHiddenDisplayCodes()
        refreshDisplaySelection()
        rebuildSignalItems()
    }

    fun restoreLastRemovedDisplayCode() {
        val code = lastRemovedDisplayCode ?: return
        hiddenDisplayCodes.remove(code)
        lastRemovedDisplayCode = null
        saveHiddenDisplayCodes()
        refreshDisplaySelection()
        rebuildSignalItems()
    }

    fun resetDisplayCodesToday() {
        hiddenDisplayCodes.clear()
        lastRemovedDisplayCode = null
        saveHiddenDisplayCodes()
        refreshDisplaySelection()
        rebuildSignalItems()
    }

    fun getProfitDisplay(code: String, currentPrice: Double): ProfitDisplay {
        val holding = holdings[code] ?: return ProfitDisplay("")
        val profit = currentPrice - holding.buyPrice
        return when {
            profit > 0.0 -> ProfitDisplay("損益 +${formatCompact(profit)}", true)
            profit < 0.0 -> ProfitDisplay("損益 ${formatCompact(profit)}", false)
            else -> ProfitDisplay("損益 0", null)
        }
    }

    fun getPriceVisual(code: String, currentPrice: Double): PriceVisual {
        val start = startPriceMap[code] ?: currentPrice
        val sameCount = samePriceCountMap[code] ?: 0
        val trend = lastTrendMap[code] ?: 0

        val bgColor = when {
            sameCount >= 3 -> R.color.skip_bg
            trend > 0 -> R.color.sell_bg
            trend < 0 -> R.color.buy_bg
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
            is ServerMessage.SignalBatch -> {
                marketItem = message.market
                val symbols: List<SignalItem> = when {
                    !message.symbols.isNullOrEmpty() -> message.symbols
                    !message.items.isNullOrEmpty() -> message.items
                    else -> emptyList()
                }
                symbols.forEach { item ->
                    updateSignalItem(item)
                }
                updateAvailableCodes()
                refreshDisplaySelection()
                rebuildSignalItems()
            }
            is ServerMessage.SignalSymbolMessage -> {
                message.symbol?.let { item ->
                    updateSignalItem(item)
                    updateAvailableCodes()
                    refreshDisplaySelection()
                    rebuildSignalItems()
                }
            }
            else -> appendUserLog(line)
        }
    }

    private fun updateSignalItem(item: SignalItem) {
        val code = extractItemCode(item)
        if (code.isBlank()) return
        val price = item.price ?: item.data?.price ?: 0.0
        if (!startPriceMap.containsKey(code)) {
            startPriceMap[code] = price
        }
        val oldPrice = signalMap[code]?.price ?: signalMap[code]?.data?.price ?: price
        previousPriceMap[code] = oldPrice

        when {
            price > oldPrice -> {
                lastTrendMap[code] = 1
                samePriceCountMap[code] = 0
            }
            price < oldPrice -> {
                lastTrendMap[code] = -1
                samePriceCountMap[code] = 0
            }
            else -> {
                samePriceCountMap[code] = (samePriceCountMap[code] ?: 0) + 1
                if (!lastTrendMap.containsKey(code)) lastTrendMap[code] = 0
            }
        }

        signalMap[code] = item
    }

    private fun refreshDisplaySelection() {
        val visibleCodes = signalMap.keys.filterNot { hiddenDisplayCodes.contains(it) }.take(17)
        _selectedDisplayCodes.value = visibleCodes
        _selectedDisplayLabels.value = visibleCodes.map { buildDisplayLabel(it) }
        _canUndoDisplayRemoval.value = lastRemovedDisplayCode != null
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
        val score = item.score ?: item.signal_score ?: item.data?.signal_score ?: 0
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

    private fun buildDisplayLabel(code: String): String {
        val item = signalMap[code]
        val name = item?.name ?: item?.data?.name ?: ""
        return if (name.isBlank()) code else "$code ${shortName(name)}"
    }

    private fun shortName(name: String): String = if (name.length <= 10) name else name.take(10) + "…"
    private fun extractItemCode(item: SignalItem): String = item.code ?: item.data?.code ?: ""
    private fun extractCode(label: String): String = label.substringBefore(" ").trim()

    private fun rememberHistory(code: String, price: Double, updatedAt: String, reason: String, score: Int) {
        val list = historyMap.getOrPut(code) { mutableListOf() }
        list.add("${updatedAt.padEnd(8)}  ${formatCompact(price).padStart(8)}  score ${score}  $reason")
        while (list.size > 30) list.removeAt(0)
    }

    private fun loadHiddenDisplayCodes() {
        hiddenDisplayCodes.clear()
        hiddenDisplayCodes.addAll(
            prefs.getString("hidden_display_codes", "")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        )
    }

    private fun saveHiddenDisplayCodes() {
        prefs.edit()
            .putString("hidden_display_codes", hiddenDisplayCodes.joinToString(","))
            .putString("display_codes_day", dayKeyFormat.format(Date()))
            .apply()
    }

    private fun rotateDayIfNeeded() {
        val today = dayKeyFormat.format(Date())
        val saved = prefs.getString("display_codes_day", "") ?: ""
        if (saved != today) {
            prefs.edit()
                .remove("hidden_display_codes")
                .remove("display_codes")
                .putString("display_codes_day", today)
                .apply()
            hiddenDisplayCodes.clear()
            lastRemovedDisplayCode = null
        }
    }

    private fun loadHoldings() {
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("holding_")) {
                val code = k.removePrefix("holding_")
                val raw = v?.toString().orEmpty()
                val parts = raw.split("|")
                val buyPrice = when {
                    parts.size >= 2 -> parts[1].toDoubleOrNull() ?: 0.0
                    else -> raw.toDoubleOrNull() ?: 0.0
                }
                if (buyPrice > 0.0) {
                    holdings[code] = HoldingInfo(buyPrice = buyPrice)
                }
            }
        }
    }

    private fun saveHoldings() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("holding_") }.forEach { editor.remove(it) }
        holdings.forEach { (code, info) ->
            editor.putString("holding_$code", info.buyPrice.toString())
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
        return if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(Locale.US, v)
    }

    override fun onCleared() {
        socketClient.stopAsync()
        super.onCleared()
    }
}
