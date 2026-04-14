//app/src/main/java/com/daytradchat/papa/network/SocketClient.kt
//ver 2.13-00
package com.daytradchat.papa.network

import com.daytradchat.papa.model.GetNowMessage
import com.daytradchat.papa.model.PingMessage
import com.daytradchat.papa.model.RegisterMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
                        SocketConfig.CONNECT_TIMEOUT_MS
                    )
                    socket = s
                    writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))

                    onStatusChanged("接続済")
                    sendJson(gson.toJson(RegisterMessage()))
                    sendJson(gson.toJson(GetNowMessage()))
                    startPingLoop()

                    val reader = InputStreamReader(s.getInputStream(), Charsets.UTF_8)
                    val buf = CharArray(1024)
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

                    onSystemLog("DISCONNECTED_BY_REMOTE_OR_EOF")
                } catch (e: Exception) {
                    onSystemLog("SOCKET_ERROR: ${e.message ?: "unknown"}")
                } finally {
                    stopPingLoop()
                    closeSocket()
                }

                onStatusChanged("未接続")
                val delayMs = reconnectDelayMsProvider().coerceAtLeast(0L)
                if (delayMs > 0L) {
                    delay(delayMs)
                }
            }
        }
    }

    fun restart() {
        stopAsync()
        scope.launch {
            delay(100L)
            start()
        }
    }

    fun stopAsync() {
        scope.launch {
            stopPingLoop()
            closeSocket()
            connectionJob?.cancel()
            connectionJob = null
        }
    }

    private fun startPingLoop() {
        if (pingJob != null) return
        pingJob = scope.launch {
            while (isActive) {
                delay(SocketConfig.PING_INTERVAL_MS)
                try {
                    sendJson(gson.toJson(PingMessage()))
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun sendJson(json: String) {
        val w = writer ?: return
        w.write(json)
        w.write("\n")
        w.flush()
        onSystemLog("SEND: $json")
    }

    private fun closeSocket() {
        try { writer?.close() } catch (_: Exception) {}
        writer = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}
