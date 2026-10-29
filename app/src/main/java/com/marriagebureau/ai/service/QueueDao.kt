package com.marriagebureau.ai.service

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueMessage(message: QueueMessageEntity): Long

    @Update
    suspend fun updateQueueMessage(message: QueueMessageEntity)

    @Query("SELECT * FROM message_queue WHERE messageId = :id LIMIT 1")
    suspend fun getMessageById(id: Long): QueueMessageEntity?

    @Query("SELECT * FROM message_queue WHERE status = 'RECEIVED' ORDER BY receivedAt ASC LIMIT 10")
    suspend fun getPendingMessages(): List<QueueMessageEntity>

    @Query("SELECT * FROM message_queue WHERE whatsappIdentifier = :whatsappId AND messageText = :text AND receivedAt > :sinceTime LIMIT 1")
    suspend fun findDuplicateMessage(whatsappId: String, text: String, sinceTime: Long): QueueMessageEntity?

    @Query("SELECT * FROM message_queue ORDER BY receivedAt DESC")
    fun getAllQueueFlow(): Flow<List<QueueMessageEntity>>

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'RECEIVED'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'PROCESSING'")
    suspend fun getProcessingCount(): Int

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'SENT'")
    suspend fun getSentCount(): Int

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int

    @Query("UPDATE message_queue SET status = 'CANCELLED' WHERE status IN ('RECEIVED', 'PROCESSING', 'READY_TO_SEND', 'RETRYING')")
    suspend fun cancelAllPending()
}
