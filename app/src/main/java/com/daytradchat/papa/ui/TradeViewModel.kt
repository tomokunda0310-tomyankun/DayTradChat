//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.17-40
package com.daytradchat.papa.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.daytradchat.papa.R
import com.daytradchat.papa.model.*
import com.daytradchat.papa.network.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class ProfitDisplay(val text: String, val isPositive: Boolean? = null)
data class PriceVisual(val bgColorRes: Int, val codeNameColorRes: Int)
private data class HoldingInfo(val buyPrice: Double)

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = ServerMessageParser()
    private val configStore = ConfigStore(application)
    private val systemLogStore = SystemLogStore(application)
    private val prefs = application.getSharedPreferences("daytrade_client_state", Context.MODE_PRIVATE)

    private val signalMap = linkedMapOf<String, SignalItem>()
    private val holdings = linkedMapOf<String, HoldingInfo>()
    private val lastTrendMap = mutableMapOf<String, Int>()
    
    private val priceHistoryMap = mutableMapOf<String, MutableList<Int>>()
    private val lastPriceMap = mutableMapOf<String, Double>()

    private val hiddenDisplayCodes = linkedSetOf<String>()
    private var lastRemovedDisplayCode: String? = null

    private val _symbolsLong = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    val symbolsLong: StateFlow<List<SignalCardUiModel>> = _symbolsLong.asStateFlow()

    private val _symbolsShort = MutableStateFlow<List<SignalCardUiModel>>(emptyList())
    val symbolsShort: StateFlow<List<SignalCardUiModel>> = _symbolsShort.asStateFlow()

    private val _availableCodes = MutableStateFlow<List<String>>(emptyList())
    val availableCodes: StateFlow<List<String>> = _availableCodes.asStateFlow()

    private val _statusLeft = MutableStateFlow("未接続")
    val statusLeft = _statusLeft.asStateFlow()

    private val _statusRight = MutableStateFlow("")
    val statusRight = _statusRight.asStateFlow()

    private val _currentHost = MutableStateFlow(configStore.loadHost())
    val currentHost = _currentHost.asStateFlow()

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

    private val socketClient = SocketClient(
        hostProvider = { _currentHost.value },
        reconnectDelayMsProvider = { _reconnectSec.value.toLong() * 1000L },
        onLineReceived = { line ->
            appendSystemLog("RECV: $line")
            handleIncomingLine(line)
        },
        onStatusChanged = { status ->
            _statusLeft.value = status
            if (status == "接続済") {
                _statusRight.value = SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(Date())
            }
        },
        onSystemLog = { appendSystemLog(it) }
    )

    fun startSocket() = socketClient.start()
    fun stopSocket() = socketClient.stopAsync()

    fun saveSettingsAndReconnect(host: String, reconnectSecText: String) {
        val sec = reconnectSecText.toIntOrNull()?.coerceIn(1, 10) ?: 1
        configStore.saveHost(host)
        configStore.saveReconnectSec(sec)
        _currentHost.value = host
        _reconnectSec.value = sec
        socketClient.restart()
    }

    fun resetSettingsAndReconnect() {
        configStore.resetAll()
        _currentHost.value = configStore.loadHost()
        _reconnectSec.value = 5
        socketClient.restart()
    }

    fun sendWatchCommand(input: String) {
        if (input.isNotEmpty()) socketClient.sendRawLine("ADD $input")
    }

    fun sendAddCodes(codes: List<String>) {
        if (codes.isNotEmpty()) socketClient.sendRawLine("ADD ${codes.joinToString(",")}")
    }

    private fun handleIncomingLine(line: String) {
        val msg = parser.parse(line) ?: return
        when (msg) {
            is ServerMessage.IndexSnapshot -> {
                val idx = msg.index
                if (idx?.code == "NIKKEI225") {
                    val sign = if ((idx.change ?: 0.0) >= 0) "+" else ""
                    _statusLeft.value = "日経: ${idx.price} ($sign${idx.change})"
                }
            }
            is ServerMessage.MasterMessage -> {
                val labels = msg.symbols?.map { "${it.code} ${it.name}" } ?: emptyList()
                _availableCodes.value = labels
            }
            is ServerMessage.SignalBatch -> {
                msg.symbols?.forEach { updateSignalItem(it) }
                rebuildSignalLists()
            }
            is ServerMessage.SignalSymbolMessage -> {
                msg.symbol?.let { updateSignalItem(it) }
                rebuildSignalLists()
            }
            is ServerMessage.GetNowResponse -> {
                msg.symbols?.forEach { updateSignalItem(it) }
                rebuildSignalLists()
            }
            else -> {}
        }
    }

    private fun updateSignalItem(item: SignalItem) {
        val code = item.code ?: return
        val newPrice = item.price ?: 0.0
        val lastPrice = lastPriceMap[code] ?: newPrice

        val move = when {
            newPrice > lastPrice -> 1
            newPrice < lastPrice -> -1
            else -> 0
        }

        val history = priceHistoryMap.getOrPut(code) { mutableListOf() }
        if (move != 0) {
            history.add(0, move)
            if (history.size > 4) history.removeAt(4)
        }

        lastPriceMap[code] = newPrice
        signalMap[code] = item
        lastTrendMap[code] = when(item.side) {
            "LONG" -> -1
            "SHORT" -> 1
            else -> 0
        }
    }

    private fun rebuildSignalLists() {
        val allUiModels = signalMap.values.filterNot { hiddenDisplayCodes.contains(it.code) }.map {
            SignalCardUiModel(
                slotId = "0",
                code = it.code ?: "",
                name = it.name ?: "",
                signalType = it.side ?: "",
                score = it.score?.toInt() ?: 0,
                price = it.price ?: 0.0,
                changeRate = it.change_rate ?: 0.0,
                reasonShort = it.reason_short ?: "",
                updatedAt = it.price_time ?: "",
                profitText = "",
                isEmpty = false
            )
        }
        _symbolsLong.value = allUiModels.filter { it.signalType == "LONG" }
        _symbolsShort.value = allUiModels.filter { it.signalType == "SHORT" }
        refreshDisplaySelection()
    }

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

        val bgColor = when {
            isReboundUp -> R.color.bg_rebound_red
            isReboundDown -> R.color.bg_rebound_green
            history.size >= 4 -> R.color.bg_default_gray
            lastMove == 1 -> R.color.bg_light_red
            lastMove == -1 -> R.color.bg_light_green
            else -> R.color.bg_default_gray
        }

        return PriceVisual(bgColor, textColor)
    }

    fun applyHolding(codeLabel: String, buyPriceText: String) {
        val code = codeLabel.substringBefore(" ").trim()
        val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0
        if (buyPrice <= 0.0) holdings.remove(code) else holdings[code] = HoldingInfo(buyPrice)
        saveHoldings()
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

    private fun refreshDisplaySelection() {
        val visible = signalMap.values.filterNot { hiddenDisplayCodes.contains(it.code) }
        _selectedDisplayLabels.value = visible.map { "${it.code} ${it.name}" }
        _canUndoDisplayRemoval.value = lastRemovedDisplayCode != null
    }

    fun getProfitDisplay(code: String, currentPrice: Double): ProfitDisplay {
        val holding = holdings[code] ?: return ProfitDisplay("")
        val profit = currentPrice - holding.buyPrice
        val text = if (profit >= 0) "+${String.format("%.1f", profit)}" else String.format("%.1f", profit)
        return ProfitDisplay("損益 $text", profit >= 0)
    }

    private fun appendSystemLog(text: String) {
        val newLog = LogLineUiModel(UUID.randomUUID().toString(), text)
        _systemLogItems.value = (listOf(newLog) + _systemLogItems.value).take(500)
        systemLogStore.append(text)
    }

    private fun saveHoldings() {
        val editor = prefs.edit()
        holdings.forEach { (code, info) -> editor.putString("holding_$code", info.buyPrice.toString()) }
        editor.apply()
    }

    fun buildHistoryDialogText(code: String): String = "履歴データなし: $code"

    fun sendAddCode(code: String?) {

        if (code.isNullOrBlank()) {
            Log.e("SEND_CODE", "code is null or blank")
            return
        }

        if (code.length != 4) {
            Log.e("SEND_CODE", "invalid code length: $code")
            return
        }

        try {
            val json = JSONObject().apply {
                put("type", "add_codes")
                put("codes", JSONArray().put(code))
            }

            Log.d("SEND_CODE", json.toString())

            SocketClient.send(json.toString())

        } catch (e: Exception) {
            Log.e("SEND_CODE", "send error", e)
        }
    }
}