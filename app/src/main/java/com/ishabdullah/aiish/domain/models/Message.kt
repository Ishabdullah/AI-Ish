package com.ishabdullah.aiish.domain.models

import com.ishabdullah.aiish.data.local.MessageEntity // Import MessageEntity
import java.util.UUID

/**
 * Represents a chat message in the conversation
 */
data class Message(
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

// Extension functions to map between domain and entity models
fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = 0, // Room will auto-generate
        content = this.content,
        role = this.role,
        timestamp = this.timestamp
    )
}

fun MessageEntity.toMessage(): Message {
    return Message(
        id = this.id.toString(), // Convert Long to String for Message domain model
        content = this.content,
        role = this.role,
        timestamp = this.timestamp
    )
}
