//app/src/main/java/com/daytradchat/papa/MainViewModel.kt
//ver 1.10-10

package com.daytradchat.papa

import androidx.lifecycle.ViewModel
import com.daytradchat.papa.network.SocketClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

private const val DEFAULT_HOST = "osaka-cosplayers.net"
private const val FIXED_PORT = 5001

data class SignalUiItem(
    val code: String = "",
    val signalType: String = "",
    val score: Int = 0,
    val price: Double = 0.0,
    val vwap: Double = 0.0,
    val boardOver: Long = 0L,
    val boardUnder: Long = 0L,
    val changeRate: Double = 0.0,
    val reasonShort: String = "",
    val sentAt: String = ""
)

data class UiState(
    val host: String = DEFAULT_HOST,
    val port: Int = FIXED_PORT,
    val statusText: String = "DISCONNECTED",
    val logs: List<String> = emptyList(),
    val signalList: List<SignalUiItem> = emptyList()
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val socketClient = SocketClientManager(
        onStatusChanged = { status ->
            _uiState.value = _uiState.value.copy(statusText = status)
        },
        onRawMessage = { raw ->
            handleRawMessage(raw)
        },
        onLog = { line ->
            addLog(line)
        }
    )

    init {
        startClient()
    }

    fun startClient() {
        val s = _uiState.value
        socketClient.start(s.host, s.port)
    }

    fun restartClient() {
        socketClient.stop()
        socketClient.start(_uiState.value.host, _uiState.value.port)
    }

    fun updateHost(host: String) {
        val newHost = host.trim().ifBlank { DEFAULT_HOST }
        _uiState.value = _uiState.value.copy(host = newHost)
    }

    private fun handleRawMessage(raw: String) {
        try {
            val root = JSONObject(raw)
            val type = root.optString("type", "")

            if (type == "pong" || type == "ping" || type == "ack" || type == "server_hello") {
                addLog(raw)
                return
            }

            if (type != "signal") {
                addLog(raw)
                return
            }

            val data = root.optJSONObject("data")

            val item = SignalUiItem(
                code = data?.optString("code").orEmpty().ifBlank { root.optString("code", "") },
                signalType = data?.optString("signal_type").orEmpty().ifBlank { root.optString("signal_type", "") },
                score = if (data?.has("signal_score") == true) data.optInt("signal_score", 0) else root.optInt("signal_score", 0),
                price = if (data?.has("price") == true) data.optDouble("price", 0.0) else root.optDouble("price", 0.0),
                vwap = if (data?.has("vwap") == true) data.optDouble("vwap", 0.0) else root.optDouble("vwap", 0.0),
                boardOver = if (data?.has("board_over") == true) data.optLong("board_over", 0L) else root.optLong("board_over", 0L),
                boardUnder = if (data?.has("board_under") == true) data.optLong("board_under", 0L) else root.optLong("board_under", 0L),
                changeRate = if (data?.has("change_rate") == true) data.optDouble("change_rate", 0.0) else root.optDouble("change_rate", 0.0),
                reasonShort = data?.optString("reason_short").orEmpty().ifBlank { root.optString("reason_short", "") },
                sentAt = root.optString("sent_at").ifBlank { data?.optString("captured_at", "") ?: "" }
            )

            if (item.code.isBlank()) {
                addLog(raw)
                return
            }

            val current = _uiState.value.signalList.toMutableList()
            val idx = current.indexOfFirst { it.code == item.code }
            if (idx >= 0) {
                current[idx] = item
            } else {
                current.add(item)
            }

            val sorted = current.sortedBy { it.code }.take(9)
            _uiState.value = _uiState.value.copy(signalList = sorted)
        } catch (e: Exception) {
            addLog("parse error: ${e.message}")
        }
    }

    private fun addLog(line: String) {
        val updated = (_uiState.value.logs + line).takeLast(200)
        _uiState.value = _uiState.value.copy(logs = updated)
    }

    override fun onCleared() {
        socketClient.stop()
        super.onCleared()
    }
}
