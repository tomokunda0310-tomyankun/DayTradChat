//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.13-00
package com.daytradchat.papa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import kotlin.math.abs

class TradeViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = ServerMessageParser()
    private val configStore = ConfigStore(application)
    private val systemLogStore = SystemLogStore(application)
    private val priceMemory = mutableMapOf<String, Double>()
    private val displayDateFormat = SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.JAPAN)

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

    private val socketClient = SocketClient(
        hostProvider = { _currentHost.value },
        reconnectDelayMsProvider = { _reconnectSec.value.toLong() * 1000L },
        onLineReceived = { line ->
            appendSystemLog("RECV: $line")
            handleIncomingLine(line)
        },
        onStatusChanged = { status ->
            _statusLeft.value = status
            _hostLine.value = hostLineText()
            if (status == "接続済") {
                _statusRight.value = displayDateFormat.format(Date())
            } else if (status == "接続中" || status == "未接続" || status == "ホスト未設定") {
                if (status != "接続済") {
                    _statusRight.value = ""
                }
            }
        },
        onSystemLog = { log ->
            appendSystemLog(log)
        }
    )

    init {
        _hostLine.value = hostLineText()
        val loaded = systemLogStore.loadToday()
        _systemLogItems.value = loaded.reversed().map { LogLineUiModel(UUID.randomUUID().toString(), it) }
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
        _currentHost.value = configStore.loadHost()
        _reconnectSec.value = configStore.loadReconnectSec()
        _hostLine.value = hostLineText()
        appendSystemLog("CONFIG_RESET")
        socketClient.restart()
    }

    private fun hostLineText(): String {
        val host = _currentHost.value.trim()
        return if (host.isBlank()) {
            "host: 未設定  port: ${SocketConfig.SERVER_PORT}  reconnect: ${_reconnectSec.value}s"
        } else {
            "host: $host  port: ${SocketConfig.SERVER_PORT}  reconnect: ${_reconnectSec.value}s"
        }
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
                appendUserLog("HELLO ${message.server_time}")
            }
            is ServerMessage.Pong -> {
                appendUserLog("PONG ${message.server_time}")
            }
            is ServerMessage.AckMessage -> {
                val typeLabel = if (message.original_type.isNotBlank()) message.original_type else "register_ack"
                appendUserLog("ACK $typeLabel: ${message.message}")
            }
            is ServerMessage.ErrorMessage -> {
                val compact = buildString {
                    append("ERROR ")
                    append(message.message)
                    if (!message.detail.isNullOrBlank()) {
                        append(" / ")
                        append(message.detail)
                    }
                }
                appendUserLog(compact)
            }
            is ServerMessage.SignalBatch -> {
                appendUserLog("BATCH ${message.sent_at} count=${message.items.size}")

                val result = mutableListOf<SignalCardUiModel>()
                val market = message.market
                if (market != null) {
                    result.add(marketToUi("slot_0", market))
                } else {
                    val firstMarket = message.items.firstOrNull { it.type.equals("market", true) || it.type.equals("index", true) }
                    result.add(if (firstMarket != null) signalItemMarketToUi("slot_0", firstMarket) else emptyIndex("slot_0"))
                }

                val stockItems = message.items.filterNot { it.type.equals("market", true) || it.type.equals("index", true) }
                stockItems.take(8).forEachIndexed { idx, item ->
                    result.add(signalToUi("slot_${idx + 1}", item))
                }

                while (result.size < 9) {
                    result.add(
                        SignalCardUiModel(
                            slotId = "slot_${result.size}",
                            codeName = "-- 待機中",
                            priceText = "0.0",
                            deltaText = "差分 0.0",
                            sub1 = "前日比 0.0%",
                            sub2 = "データ待機",
                            updatedAt = "",
                            signalType = "SKIP",
                            changeRate = 0.0,
                            isEmpty = true
                        )
                    )
                }

                _signalItems.value = result.take(9)
            }
        }
    }

    private fun marketToUi(slotId: String, market: MarketItem): SignalCardUiModel {
        val code = market.code.ifBlank { "NIKKEI225" }
        val name = market.name.ifBlank { "日経平均" }
        val price = market.price ?: 0.0
        val delta = priceDelta(code, price)
        return SignalCardUiModel(
            slotId = slotId,
            codeName = "$code $name",
            priceText = formatPrice(price),
            deltaText = "差分 ${formatSigned(delta)}",
            sub1 = "前日比 ${formatSigned(market.change_rate ?: 0.0)}%",
            sub2 = "日経平均",
            updatedAt = shortTime(market.captured_at),
            signalType = "INDEX",
            changeRate = market.change_rate ?: 0.0,
            isEmpty = false
        )
    }

    private fun signalItemMarketToUi(slotId: String, item: SignalItem): SignalCardUiModel {
        val code = item.code.orEmpty().ifBlank { "NIKKEI225" }
        val name = item.name.orEmpty().ifBlank { "日経平均" }
        val price = item.price ?: 0.0
        val delta = priceDelta(code, price)
        return SignalCardUiModel(
            slotId = slotId,
            codeName = "$code $name",
            priceText = formatPrice(price),
            deltaText = "差分 ${formatSigned(delta)}",
            sub1 = "前日比 ${formatSigned(item.change_rate ?: 0.0)}%",
            sub2 = "日経平均",
            updatedAt = shortTime(item.captured_at.orEmpty()),
            signalType = "INDEX",
            changeRate = item.change_rate ?: 0.0,
            isEmpty = false
        )
    }

    private fun signalToUi(slotId: String, item: SignalItem): SignalCardUiModel {
        val d = item.data
        val code = d.code.ifBlank { item.code.orEmpty() }.ifBlank { "--" }
        val name = normalizeName(d.name, item.title, item.body, code, d.signal_type)
        val price = d.price ?: 0.0
        val delta = priceDelta(code, price)
        val reason = d.reason_short.ifBlank { item.body.ifBlank { "理由なし" } }
        return SignalCardUiModel(
            slotId = slotId,
            codeName = "$code $name",
            priceText = formatPrice(price),
            deltaText = "差分 ${formatSigned(delta)}",
            sub1 = "前日比 ${formatSigned(d.change_rate ?: 0.0)}%",
            sub2 = reason,
            updatedAt = shortTime(d.captured_at.ifBlank { item.captured_at.orEmpty() }),
            signalType = d.signal_type.uppercase().ifBlank { "SKIP" },
            changeRate = d.change_rate ?: 0.0,
            isEmpty = false
        )
    }

    private fun normalizeName(rawName: String?, title: String, body: String, code: String, signalType: String): String {
        val direct = rawName.orEmpty().trim()
        if (direct.isNotBlank()) return direct

        val titleCandidate = title
            .replace(code, "")
            .replace(signalType, "", ignoreCase = true)
            .trim()
        if (titleCandidate.isNotBlank()) return titleCandidate

        val bodyCandidate = body
            .replace(code, "")
            .replace(signalType, "", ignoreCase = true)
            .replace(Regex("score=[-]?[0-9]+"), "")
            .replace("market=BEARISH", "")
            .replace("market=BULLISH", "")
            .trim()
        if (bodyCandidate.isNotBlank()) {
            val head = bodyCandidate.split(" ").firstOrNull().orEmpty().trim()
            if (head.isNotBlank()) return head
        }

        return "名称未取得"
    }

    private fun priceDelta(code: String, price: Double): Double {
        val prev = priceMemory[code]
        priceMemory[code] = price
        return if (prev == null) 0.0 else price - prev
    }

    private fun appendUserLog(text: String) {
        _logItems.value = (listOf(LogLineUiModel(UUID.randomUUID().toString(), text)) + _logItems.value).take(80)
    }

    private fun appendSystemLog(text: String) {
        systemLogStore.append(text)
        val updated = (listOf(LogLineUiModel(UUID.randomUUID().toString(), text)) + _systemLogItems.value)
        _systemLogItems.value = updated.take(500)
    }

    private fun createEmptySlots(): List<SignalCardUiModel> {
        val list = mutableListOf<SignalCardUiModel>()
        list.add(emptyIndex("slot_0"))
        for (i in 1 until 9) {
            list.add(
                SignalCardUiModel(
                    slotId = "slot_$i",
                    codeName = "-- 待機中",
                    priceText = "0.0",
                    deltaText = "差分 0.0",
                    sub1 = "前日比 0.0%",
                    sub2 = "データ待機",
                    updatedAt = "",
                    signalType = "SKIP",
                    changeRate = 0.0,
                    isEmpty = true
                )
            )
        }
        return list
    }

    private fun emptyIndex(slotId: String): SignalCardUiModel {
        return SignalCardUiModel(
            slotId = slotId,
            codeName = "NIKKEI225 日経平均",
            priceText = "0.0",
            deltaText = "差分 0.0",
            sub1 = "前日比 0.0%",
            sub2 = "待機中",
            updatedAt = "",
            signalType = "INDEX",
            changeRate = 0.0,
            isEmpty = true
        )
    }

    private fun formatPrice(v: Double): String {
        return if (abs(v - v.toInt()) < 0.0001) {
            "%d".format(v.toInt())
        } else {
            "%.1f".format(v)
        }
    }

    private fun formatSigned(v: Double): String {
        return if (v > 0) {
            "+%.1f".format(v)
        } else {
            "%.1f".format(v)
        }
    }

    private fun shortTime(value: String): String {
        return if (value.length >= 8) value.takeLast(8) else value
    }

    private fun formatServerTime(value: String): String {
        return value.replace("-", "/")
    }

    override fun onCleared() {
        socketClient.stopAsync()
        super.onCleared()
    }
}
