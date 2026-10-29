package com.marriagebureau.ai.service

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QueueStatus {
    RECEIVED,
    PROCESSING,
    READY_TO_SEND,
    SENT,
    FAILED,
    RETRYING,
    CANCELLED
}

@Entity(
    tableName = "message_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["customerId"]),
        Index(value = ["receivedAt"])
    ]
)
data class QueueMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,
    val customerId: Long,
    val whatsappIdentifier: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val messageText: String,
    val status: QueueStatus = QueueStatus.RECEIVED,
    val attempts: Int = 0,
    val processingStartedAt: Long? = null,
    val generatedResponse: String? = null,
    val failureReason: String? = null
)
