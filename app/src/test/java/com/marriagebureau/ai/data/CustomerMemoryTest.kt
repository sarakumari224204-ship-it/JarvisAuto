package com.marriagebureau.ai.data

import com.marriagebureau.ai.data.db.dao.CustomerDao
import com.marriagebureau.ai.data.db.entity.*
import com.marriagebureau.ai.data.repository.CustomerRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerMemoryTest {

    private class InMemCustomerDao : CustomerDao {
        val customers = mutableMapOf<Long, CustomerEntity>()
        val messages = mutableListOf<ConversationMessageEntity>()
        val requirements = mutableMapOf<Long, CustomerRequirementEntity>()
        var customerIdGen = 1L
        var msgIdGen = 1L
        var reqIdGen = 1L

        override suspend fun insertCustomer(customer: CustomerEntity): Long {
            val id = if (customer.customerId == 0L) customerIdGen++ else customer.customerId
            val saved = customer.copy(customerId = id)
            customers[id] = saved
            return id
        }

        override suspend fun updateCustomer(customer: CustomerEntity) {
            customers[customer.customerId] = customer
        }

        override suspend fun getCustomerById(id: Long): CustomerEntity? = customers[id]

        override suspend fun getCustomerByWhatsappId(identifier: String): CustomerEntity? {
            return customers.values.firstOrNull { it.whatsappIdentifier == identifier }
        }

        override fun getAllCustomersFlow(): Flow<List<CustomerEntity>> = flowOf(customers.values.toList())

        override fun getHumanRequiredCustomersFlow(): Flow<List<CustomerEntity>> {
            return flowOf(customers.values.filter { it.handoffStatus == HandoffStatus.HUMAN_REQUIRED })
        }

        override suspend fun insertMessage(message: ConversationMessageEntity): Long {
            val id = if (message.messageId == 0L) msgIdGen++ else message.messageId
            val saved = message.copy(messageId = id)
            messages.add(saved)
            return id
        }

        override suspend fun getMessagesForCustomer(customerId: Long, limit: Int, offset: Int): List<ConversationMessageEntity> {
            return messages.filter { it.customerId == customerId }.drop(offset).take(limit)
        }

        override suspend fun getRecentMessages(customerId: Long, limit: Int): List<ConversationMessageEntity> {
            return messages.filter { it.customerId == customerId }.takeLast(limit)
        }

        override suspend fun insertOrUpdateRequirement(requirement: CustomerRequirementEntity): Long {
            val id = if (requirement.requirementId == 0L) reqIdGen++ else requirement.requirementId
            val saved = requirement.copy(requirementId = id)
            requirements[requirement.customerId] = saved
            return id
        }

        override suspend fun getRequirementByCustomerId(customerId: Long): CustomerRequirementEntity? {
            return requirements[customerId]
        }

        override suspend fun updateHandoffStatus(customerId: Long, status: HandoffStatus, timestamp: Long) {
            customers[customerId]?.let {
                customers[customerId] = it.copy(handoffStatus = status, updatedAt = timestamp)
            }
        }

        override suspend fun clearCustomerMessages(customerId: Long) {
            messages.removeAll { it.customerId == customerId }
        }

        override suspend fun deleteCustomer(customerId: Long) {
            customers.remove(customerId)
            messages.removeAll { it.customerId == customerId }
            requirements.remove(customerId)
        }
    }

    private lateinit var dao: InMemCustomerDao
    private lateinit var repository: CustomerRepositoryImpl

    @Before
    fun setUp() {
        dao = InMemCustomerDao()
        repository = CustomerRepositoryImpl(dao)
    }

    @Test
    fun testMultiTurnRequirementAccumulation() = runBlocking {
        val customer = repository.getOrCreateCustomer("+919876543210", "Ramesh")

        // Turn 1
        repository.addMessage(customer.customerId, MessageSender.CUSTOMER, "Mujhe 25 saal ki ladki chahiye.")
        var req = repository.getRequirement(customer.customerId)
        assertNotNull(req)
        assertEquals("FEMALE", req?.targetGender)

        // Turn 2
        repository.addMessage(customer.customerId, MessageSender.CUSTOMER, "Gorakhpur mein ho to batao.")
        req = repository.getRequirement(customer.customerId)
        assertEquals("FEMALE", req?.targetGender)
        assertTrue(req?.preferredCities?.contains("Gorakhpur") == true)

        // Turn 3
        repository.addMessage(customer.customerId, MessageSender.CUSTOMER, "Education B.Tech honi chahiye.")
        req = repository.getRequirement(customer.customerId)
        assertEquals("FEMALE", req?.targetGender)
        assertTrue(req?.preferredCities?.contains("Gorakhpur") == true)
        assertTrue(req?.preferredEducation?.contains("B.TECH") == true)
    }

    @Test
    fun testTrackingShownProfileCodes() = runBlocking {
        val customer = repository.getOrCreateCustomer("+919876543210", "Suresh")
        repository.addShownProfileCode(customer.customerId, "MB101")
        repository.addShownProfileCode(customer.customerId, "MB102")

        val req = repository.getRequirement(customer.customerId)
        assertTrue(req?.shownProfileCodes?.contains("MB101") == true)
        assertTrue(req?.shownProfileCodes?.contains("MB102") == true)
    }
}
