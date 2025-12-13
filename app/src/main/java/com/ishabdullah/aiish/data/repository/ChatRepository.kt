package com.ishabdullah.aiish.data.repository

import com.ishabdullah.aiish.data.local.ConversationDao
import com.ishabdullah.aiish.domain.models.Message
import com.ishabdullah.aiish.domain.models.toEntity
import com.ishabdullah.aiish.domain.models.toMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val conversationDao: ConversationDao) {

    val getAllMessages: Flow<List<Message>> = conversationDao.getAllConversations().map { entities ->
        entities.map { it.toMessage() }
    }

    suspend fun insertMessage(message: Message) {
        conversationDao.insertMessage(message.toEntity())
    }

    suspend fun clearAllMessages() {
        conversationDao.clearAllConversations()
    }
}
