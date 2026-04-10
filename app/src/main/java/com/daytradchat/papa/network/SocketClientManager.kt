//app/src/main/java/com/daytradchat/papa/network/SocketClientManager.kt
//ver 1.00-13

package com.daytradchat.papa.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class SocketClientManager(
    private val onStatusChanged: (String) -> Unit,
    private val onRawMessage: (String) -> Unit,
    private val onLog: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var workerJob: Job? = null
    private var pingJob: Job? = null
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private val running = AtomicBoolean(false)

    fun start(host: String, port: Int) {
        if (running.get()) return
        running.set(true)

        workerJob = scope.launch {
            while (isActive && running.get()) {
                connectLoop(host, port)
                if (running.get()) {
                    onStatusChanged("RECONNECTING")
                    onLog("reconnect wait 5s")
                    delay(5000)
                }
            }
        }
    }

    fun stop() {
        running.set(false)

        scope.launch {
            try {
                pingJob?.cancel()
                pingJob = null
                closeSocket()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun connectLoop(host: String, port: Int) {
        onStatusChanged("CONNECTING")
        onLog("connecting $host:$port")

        try {
            closeSocket()

            val s = Socket()
            s.keepAlive = true
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(host, port), 8000)

            val r = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val w = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))

            socket = s
            writer = w

            onStatusChanged("CONNECTED")
            onLog("connected $host:$port")

            sendRegister()

            startPingLoop()

            while (scope.isActive && running.get() && !s.isClosed) {
                val line = r.readLine() ?: break
                if (line.isNotBlank()) {
                    onRawMessage(line)
                }
            }

            onLog("socket closed by peer")
        } catch (e: SocketException) {
            onLog("socket error: ${e.message}")
        } catch (e: Exception) {
            onLog("connect error: ${e.javaClass.simpleName}: ${e.message}")
        } finally {
            try {
                pingJob?.cancel()
                pingJob = null
                closeSocket()
            } catch (_: Exception) {
            }
            if (running.get()) {
                onStatusChanged("DISCONNECTED")
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()

        pingJob = scope.launch {
            while (isActive && running.get()) {
                delay(30000)
                sendPing()
            }
        }
    }

    private fun sendRegister() {
        try {
            val json = JSONObject().apply {
                put("type", "register")
                put("client_name", "android_client")
                put("client_version", "1.0.0")
            }
            sendLine(json.toString())
            onLog("register sent")
        } catch (e: Exception) {
            onLog("register send error: ${e.message}")
        }
    }

    private fun sendPing() {
        try {
            val json = JSONObject().apply {
                put("type", "ping")
            }
            sendLine(json.toString())
            onLog("ping sent")
        } catch (e: Exception) {
            onLog("ping send error: ${e.message}")
        }
    }

    private fun sendLine(text: String) {
        val w = writer ?: return
        w.write(text)
        w.write("\n")
        w.flush()
    }

    private fun closeSocket() {
        try {
            writer?.close()
        } catch (_: Exception) {
        }
        writer = null

        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }
}