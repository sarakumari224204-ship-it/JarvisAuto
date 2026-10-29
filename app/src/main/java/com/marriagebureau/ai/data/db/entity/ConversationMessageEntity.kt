package com.marriagebureau.ai.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageSender {
    CUSTOMER,
    AGENT,
    SYSTEM
}

@Entity(
    tableName = "conversation_messages",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"])
    ]
)
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,
    val customerId: Long,
    val sender: MessageSender,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)
