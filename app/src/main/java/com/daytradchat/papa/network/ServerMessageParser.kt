//app/src/main/java/com/daytradchat/papa/network/ServerMessageParser.kt
//ver 2.16-22
package com.daytradchat.papa.network

import com.daytradchat.papa.model.ServerMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class ServerMessageParser {
    private val gson = Gson()

    fun parse(line: String): ServerMessage? {
        return try {
            val root = JsonParser.parseString(line).asJsonObject
            when (root.stringValue("type")) {
                "server_hello" -> gson.fromJson(line, ServerMessage.ServerHello::class.java)
                "pong" -> gson.fromJson(line, ServerMessage.Pong::class.java)
                "ack", "register_ack", "add_codes_ack" -> gson.fromJson(line, ServerMessage.AckMessage::class.java)
                "error" -> gson.fromJson(line, ServerMessage.ErrorMessage::class.java)
                "master" -> gson.fromJson(line, ServerMessage.MasterMessage::class.java)
                "watch_update_ack" -> gson.fromJson(line, ServerMessage.WatchUpdateAckMessage::class.java)
                "signal_batch" -> gson.fromJson(line, ServerMessage.SignalBatch::class.java)
                "signal_symbol" -> gson.fromJson(line, ServerMessage.SignalSymbolMessage::class.java)
                "index_snapshot" -> gson.fromJson(line, ServerMessage.IndexSnapshot::class.java)
                "get_now_response" -> gson.fromJson(line, ServerMessage.GetNowResponse::class.java)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.stringValue(key: String): String {
        val e = get(key) ?: return ""
        return try {
            if (e.isJsonNull) "" else e.asString.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}
