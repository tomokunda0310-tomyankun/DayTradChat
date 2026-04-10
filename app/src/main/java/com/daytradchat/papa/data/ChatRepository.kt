// /app/src/main/java/com/daytradchat/papa/data/ChatRepository.kt
// ver 1.00-05
package com.daytradchat.papa.data

import com.daytradchat.papa.network.IncomingEnvelope
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatMessageDao: ChatMessageDao,
    private val appLogDao: AppLogDao
) {

    fun observeAllMessages(): Flow<List<ChatMessage>> = chatMessageDao.observeAll()

    fun observeRecentMessages(limit: Int): Flow<List<ChatMessage>> = chatMessageDao.observeRecent(limit)

    fun observeLogs(limit: Int): Flow<List<AppLog>> = appLogDao.observeRecent(limit)

    suspend fun saveEnvelope(envelope: IncomingEnvelope) {
        if (envelope.type == "pong" || envelope.type == "ping") return
        chatMessageDao.insert(
            ChatMessage(
                type = envelope.type,
                title = envelope.title,
                body = envelope.body,
                sentAt = envelope.sentAt,
                serverTime = envelope.serverTime,
                rawJson = envelope.rawJson,
                code = envelope.code,
                signalType = envelope.signalType,
                signalScore = envelope.signalScore,
                marketBias = envelope.marketBias,
                reasonShort = envelope.reasonShort,
                price = envelope.price
            )
        )
    }

    suspend fun addLog(level: String, message: String) {
        appLogDao.insert(AppLog(level = level, message = message))
    }

    suspend fun clearLogs() {
        appLogDao.clearAll()
    }
}
