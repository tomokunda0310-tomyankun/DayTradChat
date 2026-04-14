//app/src/main/java/com/daytradchat/papa/ui/TradeViewModel.kt
//ver 2.13-13

package com.daytradchat.papa.ui

import androidx.lifecycle.ViewModel
import com.daytradchat.papa.model.LogLineUiModel
import com.daytradchat.papa.model.SignalCardUiModel
import com.daytradchat.papa.model.SignalHistoryUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

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

    private val historyMap = linkedMapOf<String, MutableList<SignalHistoryUiModel>>()

    fun startSocket() {
        _statusLeft.value = "connected"
        _statusRight.value = timeFormat.format(Date())
        _hostLine.value = "host: ${_currentHost.value}:5001"

        appendSystemLog("startSocket")
        appendLog("socket started")

        val signals = buildDummySignals()
        _signalItems.value = signals
        recordHistory(signals)
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

    fun buildHistoryDialogText(code: String): String {
        val rows = historyMap[code].orEmpty()
        if (rows.isEmpty()) return "履歴なし"

        val sparkline = buildSparkline(rows.map { it.price })
        val lines = mutableListOf<String>()
        lines += "価格推移"
        lines += sparkline
        lines += ""
        lines += "時刻        価格      前日比   score  内容"

        rows.asReversed().take(10).forEach { row ->
            val time = row.time.padEnd(8, ' ')
            val price = formatCompact(row.price).padStart(8, ' ')
            val rate = "${formatSigned(row.changeRate)}%".padStart(7, ' ')
            val score = row.score.toString().padStart(5, ' ')
            lines += "$time  $price  $rate  $score  ${row.reasonShort}"
        }
        return lines.joinToString("\n")
    }

    private fun appendLog(text: String) {
        val next = mutableListOf<LogLineUiModel>()
        next.add(LogLineUiModel(UUID.randomUUID().toString(), "${shortTimeFormat.format(Date())} $text"))
        next.addAll(_logItems.value)
        _logItems.value = next.take(100)
    }

    private fun appendSystemLog(text: String) {
        val next = mutableListOf<LogLineUiModel>()
        next.add(LogLineUiModel(UUID.randomUUID().toString(), "${timeFormat.format(Date())} $text"))
        next.addAll(_systemLogItems.value)
        _systemLogItems.value = next.take(200)
    }

    private fun recordHistory(items: List<SignalCardUiModel>) {
        items.forEach { item ->
            val list = historyMap.getOrPut(item.code) { mutableListOf() }
            list.add(SignalHistoryUiModel(item.updatedAt.takeLast(8), item.price, item.changeRate, item.score, item.reasonShort))
            while (list.size > 30) list.removeAt(0)
        }
    }

    private fun buildDummySignals(): List<SignalCardUiModel> {
        val now = shortTimeFormat.format(Date())
        return listOf(
            SignalCardUiModel("0", "NIKKEI225", "日経平均", "INDEX", 0, 57895.24, -2.34, "地合い情報", now, false),
            SignalCardUiModel("1", "9434", "ソフトバンク", "SKIP", 0, 214.8, 0.0, "VWAP同値 / 板拮抗", now, false),
            SignalCardUiModel("2", "3103", "ユニチカ", "BUY", 4, 2161.0, 0.0, "VWAP上 / 板拮抗 / スプレッド良", now, false),
            SignalCardUiModel("3", "8306", "三菱ＵＦＪフィナンシャル・グループ", "SKIP", 0, 2852.5, 0.0, "VWAP下 / 板拮抗 / スプレッド良", now, false),
            SignalCardUiModel("4", "7211", "三菱自動車工業", "BUY", 4, 314.3, 0.0, "VWAP上 / 板拮抗 / スプレッド良", now, false),
            SignalCardUiModel("5", "3436", "ＳＵＭＣＯ", "BUY", 4, 2202.0, 0.0, "VWAP上 / 板拮抗 / スプレッド良", now, false),
            SignalCardUiModel("6", "7201", "日産自動車", "SKIP", 0, 349.9, 0.0, "VWAP下 / 板拮抗 / スプレッド良", now, false),
            SignalCardUiModel("7", "8136", "サンリオ", "BUY", 4, 993.2, 0.0, "VWAP上 / 板拮抗 / スプレッド良", now, false),
            SignalCardUiModel("8", "3350", "メタプラネット", "SKIP", -1, 329.0, 0.0, "VWAP下 / 板拮抗 / 出来高大", now, false)
        )
    }

    private fun emptySignalItems(): List<SignalCardUiModel> {
        return (0 until 9).map { index ->
            SignalCardUiModel(index.toString(), "--", "待機中", "SKIP", 0, 0.0, 0.0, "データ待ち", "", true)
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
}
