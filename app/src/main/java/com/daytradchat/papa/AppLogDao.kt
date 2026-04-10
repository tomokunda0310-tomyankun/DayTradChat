// /app/src/main/java/com/daytradchat/papa/data/AppLogDao.kt
// ver 1.00-00
package com.daytradchat.papa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppLog)

    @Query("SELECT * FROM app_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AppLog>>

    @Query("DELETE FROM app_logs")
    suspend fun clearAll()
}
