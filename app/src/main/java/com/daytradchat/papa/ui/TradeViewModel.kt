//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.13-08

package com.daytradchat.papa.ui

import androidx.lifecycle.ViewModel
import com.daytradchat.papa.model.LogLineUiModel
import com.daytradchat.papa.model.SignalCardUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TradeViewModel : ViewModel() {

    private val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _statusLeft = MutableStateFlow("disconnected")
    val statusLeft: StateFlow<String> = _statusLeft.asStateFlow()

    private val _statusRight = MutableStateFlow("")
    val statusRight: StateFlow<String> = _statusRight.asStateFlow()

    private val _hostLine = MutableStateFlow("host: osaka-cosplayers.net:5001")
    val hostLine: StateFlow<String> = _hostLine.asStateFlow()

    private val _currentHost = MutableStateFlow("osaka-cosplayers.net")
    val currentHost: StateFlow<String> = _currentHost.asStateFlow()

    private val _reconnectSec = MutableStateFlow(0)
    val reconnectSec: StateFlow<Int> = _reconnectSec.asStateFlow()

    private val _signalItems = MutableStateFlow<List<SignalCardUiModel>>(emptySignalItems())
    val signalItems: StateFlow<List<SignalCardUiModel>> = _signalItems.asStateFlow()

    private val _logItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    val logItems: StateFlow<List<LogLineUiModel>> = _logItems.asStateFlow()

    private val _systemLogItems = MutableStateFlow<List<LogLineUiModel>>(emptyList())
    val systemLogItems: StateFlow<List<LogLineUiModel>> = _systemLogItems.asStateFlow()

    fun startSocket() {
        _statusLeft.value = "connected"
        _statusRight.value = timeFormat.format(Date())
        _hostLine.value = "host: ${_currentHost.value}:5001"

        appendSystemLog("startSocket")
        appendLog("socket started")

        // 仮表示データ
        _signalItems.value = buildDummySignals()
    }

    fun stopSocket() {
        _statusLeft.value = "disconnected"
        appendSystemLog("stopSocket")
        appendLog("socket stopped")
    }

    fun saveSettingsAndReconnect(host: String, reconnectSec: Int) {
        val normalizedHost = host.trim().ifBlank { "osaka-cosplayers.net" }
        val normalizedReconnectSec = reconnectSec.coerceAtLeast(0)

        _currentHost.value = normalizedHost
        _reconnectSec.value = normalizedReconnectSec
        _hostLine.value = "host: ${_currentHost.value}:5001"
        _statusRight.value = timeFormat.format(Date())

        appendSystemLog("saveSettingsAndReconnect host=${_currentHost.value} reconnectSec=${_reconnectSec.value}")
        appendLog("settings saved")

        startSocket()
    }

    fun resetSettingsAndReconnect() {
        _currentHost.value = "osaka-cosplayers.net"
        _reconnectSec.value = 0
        _hostLine.value = "host: ${_currentHost.value}:5001"
        _statusRight.value = timeFormat.format(Date())

        appendSystemLog("resetSettingsAndReconnect")
        appendLog("settings reset")

        startSocket()
    }

    private fun appendLog(text: String) {
        val next = mutableListOf<LogLineUiModel>()
        next.add(
            LogLineUiModel(
                id = UUID.randomUUID().toString(),
                text = "${shortTimeFormat.format(Date())} $text"
            )
        )
        next.addAll(_logItems.value)
        _logItems.value = next.take(100)
    }

    private fun appendSystemLog(text: String) {
        val next = mutableListOf<LogLineUiModel>()
        next.add(
            LogLineUiModel(
                id = UUID.randomUUID().toString(),
                text = "${timeFormat.format(Date())} $text"
            )
        )
        next.addAll(_systemLogItems.value)
        _systemLogItems.value = next.take(200)
    }

    private fun buildDummySignals(): List<SignalCardUiModel> {
        val now = shortTimeFormat.format(Date())
        return listOf(
            SignalCardUiModel(
                slotId = "0",
                code = "NIKKEI225",
                name = "日経平均",
                signalType = "INDEX",
                score = 0,
                price = 57895.24,
                changeRate = -2.34,
                reasonShort = "地合い情報",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "1",
                code = "9434",
                name = "ソフトバンク",
                signalType = "SKIP",
                score = 0,
                price = 214.8,
                changeRate = 0.0,
                reasonShort = "VWAP同値 / 板拮抗",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "2",
                code = "3103",
                name = "ユニチカ",
                signalType = "BUY",
                score = 4,
                price = 2161.0,
                changeRate = 0.0,
                reasonShort = "VWAP上 / 板拮抗 / スプレッド良",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "3",
                code = "8306",
                name = "三菱ＵＦＪフィナンシャル・グループ",
                signalType = "SKIP",
                score = 0,
                price = 2852.5,
                changeRate = 0.0,
                reasonShort = "VWAP下 / 板拮抗 / スプレッド良",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "4",
                code = "7211",
                name = "三菱自動車工業",
                signalType = "BUY",
                score = 4,
                price = 314.3,
                changeRate = 0.0,
                reasonShort = "VWAP上 / 板拮抗 / スプレッド良",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "5",
                code = "3436",
                name = "ＳＵＭＣＯ",
                signalType = "BUY",
                score = 4,
                price = 2202.0,
                changeRate = 0.0,
                reasonShort = "VWAP上 / 板拮抗 / スプレッド良",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "6",
                code = "7201",
                name = "日産自動車",
                signalType = "SKIP",
                score = 0,
                price = 349.9,
                changeRate = 0.0,
                reasonShort = "VWAP下 / 板拮抗 / スプレッド良",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "7",
                code = "8136",
                name = "サンリオ",
                signalType = "BUY",
                score = 4,
                price = 993.2,
                changeRate = 0.0,
                reasonShort = "VWAP上 / 板拮抗 / スプレッド良",
                updatedAt = now,
                isEmpty = false
            ),
            SignalCardUiModel(
                slotId = "8",
                code = "3350",
                name = "メタプラネット",
                signalType = "SKIP",
                score = -1,
                price = 329.0,
                changeRate = 0.0,
                reasonShort = "VWAP下 / 板拮抗 / 出来高大",
                updatedAt = now,
                isEmpty = false
            )
        )
    }

    private fun emptySignalItems(): List<SignalCardUiModel> {
        return (0 until 9).map { index ->
            SignalCardUiModel(
                slotId = index.toString(),
                code = "--",
                name = "待機中",
                signalType = "SKIP",
                score = 0,
                price = 0.0,
                changeRate = 0.0,
                reasonShort = "データ待ち",
                updatedAt = "",
                isEmpty = true
            )
        }
    }
}