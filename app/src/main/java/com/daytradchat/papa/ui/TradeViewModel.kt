//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.18-01
package com.daytradchat.papa.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daytradchat.papa.R
import com.daytradchat.papa.config.ConfigStore
import com.daytradchat.papa.model.*
import com.daytradchat.papa.network.SocketClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.daytradchat.papa.network.ServerMessageParser

data class ProfitDisplay(val text: String, val isPositive: Boolean? = null)
data class PriceVisual(val bgColorRes: Int, val codeNameColorRes: Int)
private data class HoldingInfo(val buyPrice: Double)

class TradeViewModel(application: Application) : AndroidViewModel(application) {

    private val parser = ServerMessageParser()
    private val configStore = ConfigStore(application)
    private val prefs = application.getSharedPreferences("daytrade_client_state", Context.MODE_PRIVATE)

    private val signalMap = linkedMapOf<String, SignalItem>()
    private val holdings = linkedMapOf<String, HoldingInfo>()
    private val lastTrendMap = mutableMapOf<String, Int>()

    private val priceHistoryMap = mutableMapOf<String, MutableList<Int>>()
    private val lastPriceMap = mutableMapOf<String, Double>()

    private val hiddenDisplayCodes = linkedSetOf<String>()
    private var lastRemovedDisplayCode: String? = null

    private val _symbolsLong = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    val symbolsLong = _symbolsLong.asStateFlow()

    private val _symbolsShort = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    val symbolsShort = _symbolsShort.asStateFlow()

    private val _availableCodes = MutableStateFlow<List<String>>(emptyList())
    val availableCodes = _availableCodes.asStateFlow()

    private val _statusLeft = MutableStateFlow("未接続")
    val statusLeft = _statusLeft.asStateFlow()

    private val _statusRight = MutableStateFlow("")
    val statusRight = _statusRight.asStateFlow()

    private val initialSettings = configStore.loadConnectionSettings()
    private val _currentHost = MutableStateFlow(initialSettings.first)
    private val _currentPort = MutableStateFlow(initialSettings.second)
    val currentHost = _currentHost.asStateFlow()
    val currentPort = _currentPort.asStateFlow()

    private val _reconnectSec = MutableStateFlow(configStore.loadReconnectSec().coerceIn(1, 10))
    val reconnectSec = _reconnectSec.asStateFlow()

    private val _selectedDisplayLabels = MutableStateFlow<List<String>>(emptyList())
    val selectedDisplayLabels = _selectedDisplayLabels.asStateFlow()

    private val _canUndoDisplayRemoval = MutableStateFlow(false)
    val canUndoDisplayRemoval = _canUndoDisplayRemoval.asStateFlow()

    private val _logItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    val logItems = _logItems.asStateFlow()

    private val _systemLogItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    val systemLogItems = _systemLogItems.asStateFlow()

    // ★ NIKKEI225 用カード
    private var indexCard: SignalCardUiModel? = null

    // TCP版 SocketClient
    private val socketClient = SocketClient(
        hostProvider = { _currentHost.value },
        portProvider = { _currentPort.value.toInt() },
        reconnectDelayMsProvider = { _reconnectSec.value.toLong() * 1000L },

        onLineReceived = { line ->
            appendSystemLog("RECV: $line")
            handleIncomingLine(line)
        },

        onStatusChanged = { status ->
            onSocketStatus(status)
        },

        onSystemLog = { msg ->
            appendSystemLog(msg)
        }
    )

    init {
        loadHoldings()
    }

    // ==========================
    // TCP 接続
    // ==========================

    fun startSocket() = socketClient.start()

    fun stopSocket() = socketClient.stopAsync()

    fun saveSettingsAndReconnect(host: String, port: String, reconnectSecText: String) {
        val sec = reconnectSecText.toIntOrNull()?.coerceIn(1, 10) ?: 5

        viewModelScope.launch {
            configStore.saveConnectionSettings(host, port)
            configStore.saveReconnectSec(sec)

            val (newHost, newPort) = configStore.loadConnectionSettings()
            _currentHost.value = newHost
            _currentPort.value = newPort
            _reconnectSec.value = sec

            socketClient.restart()
        }
    }

    fun resetSettingsAndReconnect() {
        configStore.resetAll()
        val (host, port) = configStore.loadConnectionSettings()
        _currentHost.value = host
        _currentPort.value = port
        _reconnectSec.value = 5
        socketClient.restart()
    }

    fun sendWatchCommand(input: String) {
        if (input.isNotEmpty()) {
            socketClient.sendRawLine("ADD $input")
        }
    }

    fun sendAddCodes(codes: List<String>) {
        if (codes.isEmpty()) return

        try {
            val json = JSONObject().apply {
                put("type", "add_codes")
                val arr = JSONArray()
                for (code in codes) arr.put(code)
                put("codes", arr)
            }

            socketClient.sendRawLine(json.toString())
            appendSystemLog("SEND: $json")

        } catch (e: Exception) {
            appendSystemLog("sendAddCodes Error: ${e.message}")
        }
    }

    // ==========================
    // ソケットステータス
    // ==========================

    private fun onSocketStatus(status: String) {
        _statusLeft.value = status
        if (status == "接続済") {
            _statusRight.value = SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(Date())
        }
    }

    // ==========================
    // 保有株管理
    // ==========================

    private fun loadHoldings() {
        for ((key, value) in prefs.all) {
            if (key.startsWith("holding_")) {
                val code = key.removePrefix("holding_")
                val price = value.toString().toDoubleOrNull() ?: continue
                if (price > 0.0) holdings[code] = HoldingInfo(price)
            }
        }
    }

    private fun saveHoldings() {
        val editor = prefs.edit()
        for ((code, info) in holdings) {
            editor.putString("holding_$code", info.buyPrice.toString())
        }
        editor.apply()
    }

    fun applyHolding(codeLabel: String, buyPriceText: String) {
        val code = codeLabel.substringBefore(" ").trim()
        val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0

        if (buyPrice <= 0.0) holdings.remove(code)
        else holdings[code] = HoldingInfo(buyPrice)

        saveHoldings()
    }

    fun getProfitDisplay(code: String, currentPrice: Double): ProfitDisplay {
        val holding = holdings[code] ?: return ProfitDisplay("")
        val profit = currentPrice - holding.buyPrice
        val text = if (profit >= 0) "+${String.format("%.1f", profit)}" else String.format("%.1f", profit)
        return ProfitDisplay("損益 $text", profit >= 0)
    }

    // ==========================
    // メッセージ処理
    // ==========================

    private fun handleIncomingLine(line: String) {
        val msg = parser.parse(line) ?: return

        when (msg) {
            is ServerMessage.IndexSnapshot -> {
                val idx = msg.index ?: return
                if (idx.code == "NIKKEI225") {
                    val sign = if ((idx.change ?: 0.0) >= 0) "+" else ""
                    _statusLeft.value = "日経: ${idx.price} ($sign${idx.change})"

                    indexCard = SignalCardUiModel(
                        slotId = "index",
                        code = idx.code ?: "",
                        name = idx.name ?: "",
                        signalType = "INDEX",
                        score = idx.score?.toInt() ?: 0,
                        price = idx.price,
                        changeRate = idx.change_rate ?: 0.0,
                        reasonShort = "",
                        updatedAt = idx.captured_at ?: "",
                        profitText = "",
                        isEmpty = false,
                        judgeType = null,
                        trend = 0
                    )

                    rebuildSignalLists()
                }
            }

            is ServerMessage.MasterMessage -> {
                val list = mutableListOf<String>()
                val symbols = msg.symbols ?: emptyList()
                for (s in symbols) list.add("${s.code} ${s.name}")
                _availableCodes.value = list
            }

            is ServerMessage.SignalBatch -> {
                val symbols = msg.symbols ?: emptyList()
                for (s in symbols) updateSignalItem(s)
                rebuildSignalLists()
            }

            is ServerMessage.SignalSymbolMessage -> {
                val s = msg.symbol ?: return
                updateSignalItem(s)
                rebuildSignalLists()
            }

            is ServerMessage.GetNowResponse -> {
                val symbols = msg.symbols ?: emptyList()
                for (s in symbols) updateSignalItem(s)
                rebuildSignalLists()
            }

            else -> Unit
        }
    }

    private fun updateSignalItem(item: SignalItem) {
        val code = item.code ?: return
        val newPrice = item.price ?: return
        val lastPrice = lastPriceMap[code] ?: newPrice

        val move = when {
            newPrice > lastPrice -> 1
            newPrice < lastPrice -> -1
            else -> 0
        }

        val history = priceHistoryMap.getOrPut(code) { mutableListOf() }
        if (move != 0) {
            history.add(0, move)
            if (history.size > 4) history.removeLast()
        }

        lastPriceMap[code] = newPrice
        signalMap[code] = item

        lastTrendMap[code] = when (item.side) {
            "LONG" -> -1
            "SHORT" -> 1
            else -> 0
        }
    }

    private fun rebuildSignalLists() {
        val visibleItems = mutableListOf<SignalItem>()
        for (item in signalMap.values) {
            if (!hiddenDisplayCodes.contains(item.code)) visibleItems.add(item)
        }

        val uiModels = mutableListOf<SignalCardUiModel>()
        for (item in visibleItems) {
            val code = item.code ?: ""
            val history = priceHistoryMap[code] ?: mutableListOf()
            val lastMove = history.getOrNull(0) ?: 0

            uiModels.add(
                SignalCardUiModel(
                    slotId = "0",
                    code = code,
                    name = item.name ?: "",
                    signalType = item.side ?: "",
                    score = item.score?.toInt() ?: 0,
                    price = item.price ?: 0.0,
                    changeRate = item.change_rate ?: 0.0,
                    reasonShort = item.reason_short ?: "",
                    updatedAt = item.price_time ?: "",
                    profitText = "",
                    isEmpty = false,
                    judgeType = item.judge_type,
                    trend = lastMove
                )
            )
        }

        val longs = mutableListOf<SignalCardUiModel>()
        val shorts = mutableListOf<SignalCardUiModel>()

        for (m in uiModels) {
            if (m.signalType == "LONG") longs.add(m)
            if (m.signalType == "SHORT") shorts.add(m)
        }

        // ★ NIKKEI225 を先頭に差し込む
        indexCard?.let { idx ->
            longs.add(0, idx)
            shorts.add(0, idx)
        }

        _symbolsLong.value = longs
        _symbolsShort.value = shorts

        refreshDisplaySelection()
    }

    private fun refreshDisplaySelection() {
        val visible = mutableListOf<String>()
        for (item in signalMap.values) {
            if (!hiddenDisplayCodes.contains(item.code)) {
                visible.add("${item.code} ${item.name}")
            }
        }
        _selectedDisplayLabels.value = visible
        _canUndoDisplayRemoval.value = lastRemovedDisplayCode != null
    }

    fun removeDisplayCodeAt(position: Int) {
        val visibleList = _selectedDisplayLabels.value
        if (position in visibleList.indices) {
            val code = visibleList[position].substringBefore(" ")
            hiddenDisplayCodes.add(code)
            lastRemovedDisplayCode = code
            rebuildSignalLists()
        }
    }

    fun restoreLastRemovedDisplayCode() {
        val code = lastRemovedDisplayCode ?: return
        hiddenDisplayCodes.remove(code)
        lastRemovedDisplayCode = null
        rebuildSignalLists()
    }

    fun resetDisplayCodesToday() {
        hiddenDisplayCodes.clear()
        lastRemovedDisplayCode = null
        rebuildSignalLists()
    }

    // ★ 判定区分で上書きする PriceVisual
    fun getPriceVisual(code: String, currentPrice: Double): PriceVisual {
        val history = priceHistoryMap[code] ?: mutableListOf()
        val item = signalMap[code]
        val changeRate = item?.change_rate ?: 0.0

        val textColor = when {
            changeRate > 0 -> R.color.text_deep_red
            changeRate < 0 -> R.color.text_deep_green
            else -> R.color.text_black
        }

        val lastMove = history.getOrNull(0) ?: 0
        val isReboundUp = history.size >= 3 && history[0] == 1 && history[1] == -1 && history[2] == -1
        val isReboundDown = history.size >= 3 && history[0] == -1 && history[1] == 1 && history[2] == 1

        val baseBg = when {
            isReboundUp -> R.color.bg_rebound_red
            isReboundDown -> R.color.bg_rebound_green
            history.size >= 4 -> R.color.bg_default_gray
            lastMove == 1 -> R.color.bg_light_red
            lastMove == -1 -> R.color.bg_light_green
            else -> R.color.bg_default_gray
        }

        val judge = item?.judge_type
        val judgeBg = when (judge) {
            1 -> R.color.bg_judge_input
            2 -> R.color.bg_judge_trade
            3 -> R.color.bg_judge_hold
            4 -> R.color.bg_judge_ng
            5 -> R.color.bg_judge_ok
            else -> null
        }

        val finalBg = judgeBg ?: baseBg

        return PriceVisual(finalBg, textColor)
    }

    fun buildHistoryDialogText(code: String): String = "履歴データなし: $code"

    private fun appendSystemLog(text: String) {
        val newLog = LogLineUiModel(UUID.randomUUID().toString(), text)
        _systemLogItems.value = (listOf(newLog) + _systemLogItems.value).take(500)
    }

    fun appendUserLog(text: String) {
        val newLog = LogLineUiModel(UUID.randomUUID().toString(), text)
        _logItems.value = (listOf(newLog) + _logItems.value).take(500)
    }

    override fun onCleared() {
        super.onCleared()
        stopSocket()
    }
}
