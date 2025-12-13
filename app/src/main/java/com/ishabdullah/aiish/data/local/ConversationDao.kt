package com.ishabdullah.aiish.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllConversations(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun clearAllConversations()
}
