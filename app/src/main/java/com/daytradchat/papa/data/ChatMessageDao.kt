//app/src/main/java/com/daytradchat/papa/data/ChatMessageDao.kt
//ver 1.00-17

package com.daytradchat.papa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage): Long

    @Query("""
        SELECT * FROM chat_messages
        ORDER BY receivedAt DESC, id DESC
    """)
    fun observeAll(): Flow<List<ChatMessage>>

    @Query("""
        SELECT * FROM chat_messages
        WHERE type = 'signal'
        ORDER BY receivedAt DESC, id DESC
    """)
    fun observeSignals(): Flow<List<ChatMessage>>

    @Query("""
        DELETE FROM chat_messages
    """)
    suspend fun clearAll()

    @Query("""
        DELETE FROM chat_messages
        WHERE id NOT IN (
            SELECT id FROM chat_messages
            ORDER BY receivedAt DESC, id DESC
            LIMIT :keepCount
        )
    """)
    suspend fun trimOldMessages(keepCount: Int)
}