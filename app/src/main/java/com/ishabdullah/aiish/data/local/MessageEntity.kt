package com.ishabdullah.aiish.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ishabdullah.aiish.domain.models.MessageRole

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)
