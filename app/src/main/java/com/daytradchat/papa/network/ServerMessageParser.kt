//app/src/main/java/com/daytradchat/papa/network/ServerMessageParser.kt
//ver 2.13-00
package com.daytradchat.papa.network

import com.daytradchat.papa.model.ServerMessage
import com.google.gson.Gson
import com.google.gson.JsonParser

class ServerMessageParser {
    private val gson = Gson()

    fun parse(line: String): ServerMessage? {
        return try {
            val root = JsonParser.parseString(line).asJsonObject
            when (root.get("type")?.asString.orEmpty()) {
                "server_hello" -> gson.fromJson(line, ServerMessage.ServerHello::class.java)
                "pong" -> gson.fromJson(line, ServerMessage.Pong::class.java)
                "ack" -> gson.fromJson(line, ServerMessage.AckMessage::class.java)
                "register_ack" -> gson.fromJson(line, ServerMessage.AckMessage::class.java)
                "error" -> gson.fromJson(line, ServerMessage.ErrorMessage::class.java)
                "signal_batch" -> gson.fromJson(line, ServerMessage.SignalBatch::class.java)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
