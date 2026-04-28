// app/src/main/java/com/daytradchat/papa/network/SocketClient.kt
package com.daytradchat.papa.network

import com.daytradchat.papa.model.GetNowMessage
import com.daytradchat.papa.model.PingMessage
import com.daytradchat.papa.model.RegisterMessage
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class SocketClient(
    private val hostProvider: () -> String,
    private val reconnectDelayMsProvider: () -> Long,
    private val onLineReceived: (String) -> Unit,
    private val onStatusChanged: (String) -> Unit,
    private val onSystemLog: (String) -> Unit
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var connectionJob: Job? = null
    private var pingJob: Job? = null

    fun start() {
        if (connectionJob != null) return

        connectionJob = scope.launch {
            while (isActive) {
                val host = hostProvider().trim()
                if (host.isBlank()) {
                    onStatusChanged("ホスト未設定")
                    delay(1000L)
                    continue
                }

                try {
                    onStatusChanged("接続中")
                    val s = Socket()
                    s.connect(
                        InetSocketAddress(host, SocketConfig.SERVER_PORT),
                        3000 // タイムアウト
                    )
                    socket = s
                    writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))

                    onStatusChanged("接続済")
                    sendJson(gson.toJson(RegisterMessage()))
                    sendJson(gson.toJson(GetNowMessage()))
                    startPingLoop()

                    val reader = InputStreamReader(s.getInputStream(), Charsets.UTF_8)
                    val buf = CharArray(2048)
                    val sb = StringBuilder()

                    while (isActive && !s.isClosed) {
                        val n = reader.read(buf)
                        if (n == -1) break
                        sb.append(buf, 0, n)

                        while (true) {
                            val idx = sb.indexOf("\n")
                            if (idx < 0) break
                            val line = sb.substring(0, idx).trim()
                            sb.delete(0, idx + 1)
                            if (line.isNotBlank()) {
                                onLineReceived(line)
                            }
                        }
                    }
                    onSystemLog("切断されました")
                } catch (e: Exception) {
                    onSystemLog("接続エラー: ${e.message}")
                } finally {
                    stopPingLoop()
                    closeSocket()
                }

                onStatusChanged("再接続待機")
                delay(reconnectDelayMsProvider().coerceAtLeast(1000L))
            }
        }
    }

    fun restart() {
        stopAsync()
        scope.launch {
            delay(500L)
            start()
        }
    }

    fun stopAsync() {
        connectionJob?.cancel()
        connectionJob = null
        stopPingLoop()
        closeSocket()
    }

    fun sendRawLine(line: String) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return
        scope.launch {
            try {
                writer?.let {
                    it.write(trimmed)
                    it.write("\n")
                    it.flush()
                    onSystemLog("SEND: $trimmed")
                }
            } catch (e: Exception) {
                onSystemLog("送信エラー: ${e.message}")
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(30000L) // 30秒ごとにPing
                try {
                    sendJson(gson.toJson(PingMessage()))
                } catch (_: Exception) { }
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun sendJson(json: String) {
        writer?.let {
            it.write(json)
            it.write("\n")
            it.flush()
        }
    }

    private fun closeSocket() {
        try { writer?.close() } catch (_: Exception) {}
        writer = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}
