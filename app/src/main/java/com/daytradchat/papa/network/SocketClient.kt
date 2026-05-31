//app/src/main/java/com/daytradchat/papa/network/SocketClient.kt
//ver 2.17-48
package com.daytradchat.papa.network

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class SocketClient(
    private val hostProvider: () -> String,
    private val portProvider: () -> Int,
    private val reconnectDelayMsProvider: () -> Long,
    private val onLineReceived: (String) -> Unit,
    private val onStatusChanged: (String) -> Unit,
    private val onSystemLog: (String) -> Unit
) {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            connectLoop()
        }
    }

    fun stopAsync() {
        job?.cancel()
        job = null
        closeSocket()
    }

    fun restart() {
        stopAsync()
        start()
    }

    private suspend fun connectLoop() {
        while (job?.isActive == true) {
            try {
                val host = hostProvider()
                val port = portProvider()

                onStatusChanged("接続中…")
                onSystemLog("CONNECT TCP: $host:$port")

                socket = Socket()
                socket!!.connect(InetSocketAddress(host, port), 5000)

                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                onStatusChanged("接続済")
                onSystemLog("CONNECTED")

                while (job?.isActive == true && socket!!.isConnected) {
                    val line = reader?.readLine() ?: break
                    onLineReceived(line)
                }

            } catch (e: Exception) {
                onSystemLog("ERROR: ${e.message}")
                onStatusChanged("切断")
            }

            closeSocket()
            delay(reconnectDelayMsProvider())
        }
    }

    fun sendRawLine(text: String) {
        try {
            writer?.println(text)
            writer?.flush()
        } catch (e: Exception) {
            onSystemLog("SEND ERROR: ${e.message}")
        }
    }

    private fun closeSocket() {
        try { reader?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}

        reader = null
        writer = null
        socket = null
    }
}
