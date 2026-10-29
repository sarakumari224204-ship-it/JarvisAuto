package com.marriagebureau.ai.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WhatsAppAndQueueTest {

    private class MockQueueDao : QueueDao {
        val storage = mutableMapOf<Long, QueueMessageEntity>()
        var idCounter = 1L

        override suspend fun enqueueMessage(message: QueueMessageEntity): Long {
            val id = if (message.messageId == 0L) idCounter++ else message.messageId
            val saved = message.copy(messageId = id)
            storage[id] = saved
            return id
        }

        override suspend fun updateQueueMessage(message: QueueMessageEntity) {
            storage[message.messageId] = message
        }

        override suspend fun getMessageById(id: Long): QueueMessageEntity? = storage[id]

        override suspend fun getPendingMessages(): List<QueueMessageEntity> {
            return storage.values.filter { it.status == QueueStatus.RECEIVED }.sortedBy { it.receivedAt }
        }

        override suspend fun findDuplicateMessage(whatsappId: String, text: String, sinceTime: Long): QueueMessageEntity? {
            return storage.values.firstOrNull {
                it.whatsappIdentifier == whatsappId && it.messageText == text && it.receivedAt > sinceTime
            }
        }

        override fun getAllQueueFlow(): Flow<List<QueueMessageEntity>> = flowOf(storage.values.toList())

        override suspend fun getPendingCount(): Int = storage.values.count { it.status == QueueStatus.RECEIVED }
        override suspend fun getProcessingCount(): Int = storage.values.count { it.status == QueueStatus.PROCESSING }
        override suspend fun getSentCount(): Int = storage.values.count { it.status == QueueStatus.SENT }
        override suspend fun getFailedCount(): Int = storage.values.count { it.status == QueueStatus.FAILED }

        override suspend fun cancelAllPending() {
            storage.keys.forEach { id ->
                if (storage[id]?.status in listOf(QueueStatus.RECEIVED, QueueStatus.PROCESSING, QueueStatus.READY_TO_SEND, QueueStatus.RETRYING)) {
                    storage[id] = storage[id]!!.copy(status = QueueStatus.CANCELLED)
                }
            }
        }
    }

    private lateinit var dao: MockQueueDao
    private lateinit var repository: QueueRepositoryImpl

    @Before
    fun setUp() {
        EmergencyStopController.resetEmergencyStop()
        dao = MockQueueDao()
        repository = QueueRepositoryImpl(dao)
    }

    @Test
    fun testEnqueueAndDeduplication() = runBlocking {
        val m1 = repository.enqueueInboundMessage(1L, "9876543210", "Need female profile around 25")
        assertNotNull(m1)

        // Duplicate message sent immediately should be ignored
        val m2 = repository.enqueueInboundMessage(1L, "9876543210", "Need female profile around 25")
        assertNull(m2)

        val metrics = repository.getMetrics()
        assertEquals(1, metrics.pendingCount)
    }

    @Test
    fun testEmergencyStopSystem() = runBlocking {
        repository.enqueueInboundMessage(1L, "9876543210", "Message 1")
        repository.enqueueInboundMessage(2L, "9876543211", "Message 2")

        assertEquals(2, repository.getMetrics().pendingCount)

        repository.triggerEmergencyStop()

        assertTrue(EmergencyStopController.isEmergencyStopped)
        assertEquals(0, repository.getMetrics().pendingCount)

        // No new messages accepted during emergency stop
        val blocked = repository.enqueueInboundMessage(3L, "9876543212", "Message 3")
        assertNull(blocked)
    }
}
