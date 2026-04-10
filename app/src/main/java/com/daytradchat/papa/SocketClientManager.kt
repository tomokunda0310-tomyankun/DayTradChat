// /app/src/main/java/com/daytradchat/papa/network/SocketClientManager.kt
// ver 1.00-00
package com.daytradchat.papa.network

import com.daytradchat.papa.config.ConfigStore
import com.daytradchat.papa.data.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class SocketClientManager(
    private val repository: ChatRepository,
    private val configStore: ConfigStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _lastPongTime = MutableStateFlow<String?>(null)
    val lastPongTime: StateFlow<String?> = _lastPongTime

    fun start(host: String) {
        if (host.isBlank()) return
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch {
            connectLoop(host)
        }
    }

    fun restart(host: String) {
        stop()
        start(host)
    }

    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private suspend fun connectLoop(host: String) {
        while (scope.isActive) {
            var socket: Socket? = null
            try {
                _connectionState.value = if (_connectionState.value == ConnectionState.DISCONNECTED) {
                    ConnectionState.CONNECTING
                } else {
                    ConnectionState.RECONNECTING
                }
                repository.addLog("INFO", "CONNECT host=$host port=$FIXED_PORT")

                socket = withContext(Dispatchers.IO) {
                    Socket().apply {
                        connect(InetSocketAddress(host, FIXED_PORT), CONNECT_TIMEOUT_MS)
                        soTimeout = READ_TIMEOUT_MS
                        keepAlive = true
                    }
                }

                _connectionState.value = ConnectionState.CONNECTED
                repository.addLog("INFO", "CONNECTED")

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

                sendRegister(writer)
                val pingJob = launchPing(writer)

                try {
                    while (isActive) {
                        val line = reader.readLine() ?: throw IllegalStateException("server closed")
                        handleIncomingLine(line)
                    }
                } finally {
                    pingJob.cancel()
                }
            } catch (e: Exception) {
                repository.addLog("ERROR", e.message ?: e.javaClass.simpleName)
            } finally {
                runCatching { socket?.close() }
                if (scope.isActive) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    repository.addLog("WARN", "RECONNECT in ${RECONNECT_DELAY_MS / 1000}s")
                    delay(RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private fun launchPing(writer: BufferedWriter): Job {
        return scope.launch {
            while (isActive) {
                delay(PING_INTERVAL_MS)
                sendLine(writer, PING_JSON)
                repository.addLog("DEBUG", "PING sent")
            }
        }
    }

    private suspend fun handleIncomingLine(line: String) {
        val envelope = runCatching { parseEnvelope(line) }.getOrElse {
            repository.addLog("WARN", "JSON parse failed, raw saved")
            IncomingEnvelope(type = "raw_message", rawJson = line, body = line)
        }

        if (envelope.type == "pong") {
            _lastPongTime.value = envelope.serverTime ?: envelope.sentAt ?: "received"
        }
        repository.saveEnvelope(envelope)
    }

    private suspend fun sendRegister(writer: BufferedWriter) {
        sendLine(writer, REGISTER_JSON)
        repository.addLog("INFO", "REGISTER sent")
    }

    private suspend fun sendLine(writer: BufferedWriter, line: String) {
        withContext(Dispatchers.IO) {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }
    }

    private fun parseEnvelope(line: String): IncomingEnvelope {
        val root = json.parseToJsonElement(line).jsonObject
        val type = root.string("type").ifBlank { "unknown" }
        val data = root["data"]?.jsonObject

        return IncomingEnvelope(
            type = type,
            title = root.string("title").ifBlank { null },
            body = root.string("body").ifBlank { null },
            sentAt = root.string("sent_at").ifBlank { null },
            serverTime = root.string("server_time").ifBlank { null },
            rawJson = line,
            code = data?.string("code")?.ifBlank { null },
            signalType = data?.string("signal_type")?.ifBlank { null },
            signalScore = data?.int("signal_score"),
            marketBias = data?.string("market_bias")?.ifBlank { null },
            reasonShort = data?.string("reason_short")?.ifBlank { null },
            price = data?.double("price")
        )
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

    companion object {
        const val FIXED_PORT = 5001
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 65_000
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val PING_INTERVAL_MS = 30_000L
        private const val REGISTER_JSON =
            "{\"type\":\"register\",\"client_name\":\"android_client\",\"client_version\":\"1.0.0\"}"
        private const val PING_JSON = "{\"type\":\"ping\"}"
    }
}

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING
}

data class IncomingEnvelope(
    val type: String,
    val title: String? = null,
    val body: String? = null,
    val sentAt: String? = null,
    val serverTime: String? = null,
    val rawJson: String,
    val code: String? = null,
    val signalType: String? = null,
    val signalScore: Int? = null,
    val marketBias: String? = null,
    val reasonShort: String? = null,
    val price: Double? = null
)
