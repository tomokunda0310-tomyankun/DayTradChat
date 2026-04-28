//app/src/main/java/com/daytradchat/papa/network/SocketClient.kt
//ver 2.17-40
package com.daytradchat.papa.network

import android.util.Log
import okhttp3.*

object SocketClient {

    private var webSocket: WebSocket? = null

    fun connect(url: String) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d("SocketClient", "connected")
                    webSocket = ws
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    Log.d("SocketClient", "RECV: $text")
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e("SocketClient", "error", t)
                }
            })

        } catch (e: Exception) {
            Log.e("SocketClient", "connect error", e)
        }
    }

    fun send(message: String) {
        try {
            webSocket?.send(message)
        } catch (e: Exception) {
            Log.e("SocketClient", "Send Error", e)
        }
    }

}