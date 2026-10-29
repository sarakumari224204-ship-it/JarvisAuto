package com.marriagebureau.ai.service

import kotlinx.coroutines.flow.Flow

data class QueueMetrics(
    val pendingCount: Int,
    val processingCount: Int,
    val sentCount: Int,
    val failedCount: Int,
    val isEmergencyStopped: Boolean
)

interface QueueRepository {
    suspend fun enqueueInboundMessage(customerId: Long, whatsappId: String, text: String): QueueMessageEntity?
    suspend fun getNextPendingMessage(): QueueMessageEntity?
    suspend fun updateMessageStatus(messageId: Long, status: QueueStatus, generatedResponse: String? = null, failureReason: String? = null)
    suspend fun retryFailedMessage(messageId: Long): Boolean
    suspend fun getMetrics(): QueueMetrics
    fun getAllQueueFlow(): Flow<List<QueueMessageEntity>>
    suspend fun triggerEmergencyStop()
}

class QueueRepositoryImpl(
    private val queueDao: QueueDao
) : QueueRepository {

    override suspend fun enqueueInboundMessage(
        customerId: Long,
        whatsappId: String,
        text: String
    ): QueueMessageEntity? {
        if (EmergencyStopController.isEmergencyStopped) {
            return null
        }

        // Deduplication: ignore if identical message received within last 60 seconds
        val duplicateWindow = System.currentTimeMillis() - 60_000
        val duplicate = queueDao.findDuplicateMessage(whatsappId, text, duplicateWindow)
        if (duplicate != null) {
            return null // Ignore duplicate
        }

        val msg = QueueMessageEntity(
            customerId = customerId,
            whatsappIdentifier = whatsappId,
            messageText = text,
            status = QueueStatus.RECEIVED,
            receivedAt = System.currentTimeMillis()
        )

        val id = queueDao.enqueueMessage(msg)
        return msg.copy(messageId = id)
    }

    override suspend fun getNextPendingMessage(): QueueMessageEntity? {
        if (EmergencyStopController.isEmergencyStopped) {
            return null
        }

        val pending = queueDao.getPendingMessages()
        val next = pending.firstOrNull() ?: return null

        val processingMsg = next.copy(
            status = QueueStatus.PROCESSING,
            processingStartedAt = System.currentTimeMillis(),
            attempts = next.attempts + 1
        )
        queueDao.updateQueueMessage(processingMsg)
        return processingMsg
    }

    override suspend fun updateMessageStatus(
        messageId: Long,
        status: QueueStatus,
        generatedResponse: String?,
        failureReason: String?
    ) {
        val msg = queueDao.getMessageById(messageId) ?: return
        val updated = msg.copy(
            status = status,
            generatedResponse = generatedResponse ?: msg.generatedResponse,
            failureReason = failureReason ?: msg.failureReason
        )
        queueDao.updateQueueMessage(updated)
    }

    override suspend fun retryFailedMessage(messageId: Long): Boolean {
        if (EmergencyStopController.isEmergencyStopped) return false
        val msg = queueDao.getMessageById(messageId) ?: return false

        val retrying = msg.copy(
            status = QueueStatus.RECEIVED,
            attempts = msg.attempts,
            failureReason = null
        )
        queueDao.updateQueueMessage(retrying)
        return true
    }

    override suspend fun getMetrics(): QueueMetrics {
        return QueueMetrics(
            pendingCount = queueDao.getPendingCount(),
            processingCount = queueDao.getProcessingCount(),
            sentCount = queueDao.getSentCount(),
            failedCount = queueDao.getFailedCount(),
            isEmergencyStopped = EmergencyStopController.isEmergencyStopped
        )
    }

    override fun getAllQueueFlow(): Flow<List<QueueMessageEntity>> {
        return queueDao.getAllQueueFlow()
    }

    override suspend fun triggerEmergencyStop() {
        EmergencyStopController.triggerEmergencyStop()
        queueDao.cancelAllPending()
    }
}
