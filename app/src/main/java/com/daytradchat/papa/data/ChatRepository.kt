//app/src/main/java/com/daytradchat/papa/data/ChatRepository.kt
//ver 1.00-14

package com.daytradchat.papa.data

import org.json.JSONObject

class ChatRepository {

    fun fromRawJson(rawJson: String, receivedAt: Long = System.currentTimeMillis()): ChatMessage {
        return try {
            val root = JSONObject(rawJson)
            val type = root.optString("type", "")

            when (type) {
                "signal" -> parseSignal(root, rawJson, receivedAt)

                "server_hello" -> ChatMessage(
                    type = "server_hello",
                    rawJson = rawJson,
                    title = "HELLO",
                    body = root.optString("message", ""),
                    sentAt = root.optString("server_time", ""),
                    receivedAt = receivedAt
                )

                "ack" -> ChatMessage(
                    type = "ack",
                    rawJson = rawJson,
                    title = "ACK",
                    body = root.optString("message", ""),
                    sentAt = root.optString("server_time", ""),
                    receivedAt = receivedAt
                )

                "pong" -> ChatMessage(
                    type = "pong",
                    rawJson = rawJson,
                    title = "PONG",
                    body = "",
                    sentAt = root.optString("server_time", ""),
                    receivedAt = receivedAt
                )

                "ping" -> ChatMessage(
                    type = "ping",
                    rawJson = rawJson,
                    title = "PING",
                    body = "",
                    sentAt = root.optString("server_time", ""),
                    receivedAt = receivedAt
                )

                else -> ChatMessage(
                    type = if (type.isBlank()) "raw" else type,
                    rawJson = rawJson,
                    title = if (type.isBlank()) "RAW" else type.uppercase(),
                    body = rawJson,
                    sentAt = root.optString("server_time", root.optString("sent_at", "")),
                    receivedAt = receivedAt
                )
            }
        } catch (_: Exception) {
            ChatMessage(
                type = "raw",
                rawJson = rawJson,
                title = "RAW",
                body = rawJson,
                receivedAt = receivedAt
            )
        }
    }

    private fun parseSignal(root: JSONObject, rawJson: String, receivedAt: Long): ChatMessage {
        val data = root.optJSONObject("data")

        val code = root.optString("code").ifBlank {
            data?.optString("code", "") ?: ""
        }

        val signalType = root.optString("signal_type").ifBlank {
            data?.optString("signal_type", "") ?: ""
        }

        val score = when {
            root.has("signal_score") -> root.optInt("signal_score", 0)
            else -> data?.optInt("signal_score", 0) ?: 0
        }

        val price = when {
            root.has("price") -> root.optDouble("price", 0.0)
            else -> data?.optDouble("price", 0.0) ?: 0.0
        }

        val vwap = when {
            root.has("vwap") -> root.optDouble("vwap", 0.0)
            else -> data?.optDouble("vwap", 0.0) ?: 0.0
        }

        val boardOver = when {
            root.has("board_over") -> root.optLong("board_over", 0L)
            else -> data?.optLong("board_over", 0L) ?: 0L
        }

        val boardUnder = when {
            root.has("board_under") -> root.optLong("board_under", 0L)
            else -> data?.optLong("board_under", 0L) ?: 0L
        }

        val changeRate = when {
            root.has("change_rate") -> root.optDouble("change_rate", 0.0)
            else -> data?.optDouble("change_rate", 0.0) ?: 0.0
        }

        val reasonShort = root.optString("reason_short").ifBlank {
            data?.optString("reason_short", "") ?: ""
        }

        val reasonDetail = root.optString("reason_detail").ifBlank {
            data?.optString("reason_detail", "") ?: ""
        }

        val sentAt = root.optString("sent_at").ifBlank {
            data?.optString("captured_at", "") ?: ""
        }

        return ChatMessage(
            type = "signal",
            rawJson = rawJson,
            title = root.optString("title", "$code $signalType").trim(),
            body = root.optString("body", reasonShort).trim(),
            code = code,
            signalType = signalType,
            score = score,
            price = price,
            vwap = vwap,
            boardOver = boardOver,
            boardUnder = boardUnder,
            changeRate = changeRate,
            reasonShort = reasonShort,
            reasonDetail = reasonDetail,
            sentAt = sentAt,
            receivedAt = receivedAt
        )
    }
}