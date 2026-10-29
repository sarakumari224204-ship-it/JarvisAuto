package com.marriagebureau.ai.data.db.dao

import androidx.room.*
import com.marriagebureau.ai.data.db.entity.ConversationMessageEntity
import com.marriagebureau.ai.data.db.entity.CustomerEntity
import com.marriagebureau.ai.data.db.entity.CustomerRequirementEntity
import com.marriagebureau.ai.data.db.entity.HandoffStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE customerId = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE whatsappIdentifier = :identifier LIMIT 1")
    suspend fun getCustomerByWhatsappId(identifier: String): CustomerEntity?

    @Query("SELECT * FROM customers ORDER BY lastInteractionAt DESC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE handoffStatus = 'HUMAN_REQUIRED' ORDER BY lastInteractionAt DESC")
    fun getHumanRequiredCustomersFlow(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessageEntity): Long

    @Query("SELECT * FROM conversation_messages WHERE customerId = :customerId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForCustomer(customerId: Long, limit: Int = 50, offset: Int = 0): List<ConversationMessageEntity>

    @Query("SELECT * FROM conversation_messages WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(customerId: Long, limit: Int = 10): List<ConversationMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRequirement(requirement: CustomerRequirementEntity): Long

    @Query("SELECT * FROM customer_requirements WHERE customerId = :customerId LIMIT 1")
    suspend fun getRequirementByCustomerId(customerId: Long): CustomerRequirementEntity?

    @Query("UPDATE customers SET handoffStatus = :status, updatedAt = :timestamp WHERE customerId = :customerId")
    suspend fun updateHandoffStatus(customerId: Long, status: HandoffStatus, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversation_messages WHERE customerId = :customerId")
    suspend fun clearCustomerMessages(customerId: Long)

    @Query("DELETE FROM customers WHERE customerId = :customerId")
    suspend fun deleteCustomer(customerId: Long)
}
