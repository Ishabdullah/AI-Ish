package com.ishabdullah.aiish.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a chat message in the conversation
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "{}", // JSON string
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
