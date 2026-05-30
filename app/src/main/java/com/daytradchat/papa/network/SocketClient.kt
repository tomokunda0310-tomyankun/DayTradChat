//app/src/main/java/com/daytradchat/papa/network/SocketClient.kt
//ver 2.17-45 (class version for TradeViewModel)

package com.daytradchat.papa.network

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class SocketClient(
    private val hostProvider: () -> String,
    private val reconnectDelayMsProvider: () -> Long,
    private val onLineReceived: (String) -> Unit,
    private val onStatusChanged: (String) -> Unit,
    private val onSystemLog: (String) -> Unit
) {

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var reconnectJob: Job? = null

    @Volatile
    private var isActive = false

    private fun buildUrl(): String {
        val host = hostProvider()
        return "ws://$host/ws"
    }

    fun start() {
        isActive = true
        connect()
    }

    fun stopAsync() {
        isActive = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "stop")
        onSystemLog("Socket stopped")
    }

    fun restart() {
        stopAsync()
        start()
    }

    fun updateSettings(host: String, port: String) {
        // TradeViewModel expects this, but URL is built only from host
        onSystemLog("Settings updated: $host:$port")
    }

    fun sendRawLine(text: String) {
        try {
            webSocket?.send(text)
            onSystemLog("SEND: $text")
        } catch (e: Exception) {
            onSystemLog("Send error: ${e.message}")
        }
    }

    private fun connect() {
        val url = buildUrl()
        onSystemLog("CONNECT: $url")

        client = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        webSocket = client!!.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                onSystemLog("OPEN")
                onStatusChanged("接続済")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                onLineReceived(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                onLineReceived(bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                onSystemLog("ERROR: ${t.message}")
                onStatusChanged("切断")

                if (isActive) scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                onSystemLog("CLOSED: $reason")
                onStatusChanged("切断")
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            val delayMs = reconnectDelayMsProvider()
            onSystemLog("Reconnecting in ${delayMs}ms")
            delay(delayMs)
            if (isActive) connect()
        }
    }
}
