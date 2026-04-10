// /app/src/main/java/com/daytradchat/papa/data/ChatMessageDao.kt
// ver 1.00-00
package com.daytradchat.papa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Query("SELECT * FROM chat_messages ORDER BY displayOrderTime DESC")
    fun observeAll(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY displayOrderTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
