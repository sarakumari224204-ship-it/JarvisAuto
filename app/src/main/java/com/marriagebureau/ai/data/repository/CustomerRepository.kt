package com.marriagebureau.ai.data.repository

import com.marriagebureau.ai.data.db.dao.CustomerDao
import com.marriagebureau.ai.data.db.entity.ConversationMessageEntity
import com.marriagebureau.ai.data.db.entity.CustomerEntity
import com.marriagebureau.ai.data.db.entity.CustomerRequirementEntity
import com.marriagebureau.ai.data.db.entity.HandoffStatus
import com.marriagebureau.ai.data.db.entity.MessageSender
import kotlinx.coroutines.flow.Flow
import java.util.regex.Pattern

interface CustomerRepository {
    suspend fun getOrCreateCustomer(whatsappId: String, displayName: String = ""): CustomerEntity
    suspend fun getCustomerById(id: Long): CustomerEntity?
    suspend fun addMessage(customerId: Long, sender: MessageSender, text: String): ConversationMessageEntity
    suspend fun getRecentMessages(customerId: Long, limit: Int = 10): List<ConversationMessageEntity>
    suspend fun getRequirement(customerId: Long): CustomerRequirementEntity?
    suspend fun updateRequirementFromText(customerId: Long, text: String): CustomerRequirementEntity
    suspend fun addShownProfileCode(customerId: Long, profileCode: String)
    suspend fun setHandoffStatus(customerId: Long, status: HandoffStatus)
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>
    fun getHumanRequiredCustomersFlow(): Flow<List<CustomerEntity>>
    suspend fun clearHistory(customerId: Long)
}

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override suspend fun getOrCreateCustomer(whatsappId: String, displayName: String): CustomerEntity {
        val existing = customerDao.getCustomerByWhatsappId(whatsappId)
        if (existing != null) {
            val updated = existing.copy(
                displayName = if (displayName.isNotBlank()) displayName else existing.displayName,
                lastInteractionAt = System.currentTimeMillis()
            )
            customerDao.updateCustomer(updated)
            return updated
        }

        val newCustomer = CustomerEntity(
            whatsappIdentifier = whatsappId,
            displayName = displayName,
            lastInteractionAt = System.currentTimeMillis()
        )
        val id = customerDao.insertCustomer(newCustomer)
        val created = newCustomer.copy(customerId = id)

        // Initialize empty requirement record
        customerDao.insertOrUpdateRequirement(
            CustomerRequirementEntity(customerId = id)
        )
        return created
    }

    override suspend fun getCustomerById(id: Long): CustomerEntity? {
        return customerDao.getCustomerById(id)
    }

    override suspend fun addMessage(
        customerId: Long,
        sender: MessageSender,
        text: String
    ): ConversationMessageEntity {
        val msg = ConversationMessageEntity(
            customerId = customerId,
            sender = sender,
            messageText = text,
            timestamp = System.currentTimeMillis()
        )
        val msgId = customerDao.insertMessage(msg)

        customerDao.getCustomerById(customerId)?.let { customer ->
            customerDao.updateCustomer(
                customer.copy(
                    lastInteractionAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        if (sender == MessageSender.CUSTOMER) {
            updateRequirementFromText(customerId, text)
        }

        return msg.copy(messageId = msgId)
    }

    override suspend fun getRecentMessages(customerId: Long, limit: Int): List<ConversationMessageEntity> {
        return customerDao.getRecentMessages(customerId, limit).reversed()
    }

    override suspend fun getRequirement(customerId: Long): CustomerRequirementEntity? {
        return customerDao.getRequirementByCustomerId(customerId)
    }

    override suspend fun updateRequirementFromText(
        customerId: Long,
        text: String
    ): CustomerRequirementEntity {
        val existing = customerDao.getRequirementByCustomerId(customerId)
            ?: CustomerRequirementEntity(customerId = customerId)

        var gender = existing.targetGender
        var minAge = existing.minAge
        var maxAge = existing.maxAge
        var cities = existing.preferredCities
        var education = existing.preferredEducation

        val lowerText = text.lowercase()

        // Gender detection
        if (lowerText.contains("ladki") || lowerText.contains("girl") || lowerText.contains("female") || lowerText.contains("bride")) {
            gender = "FEMALE"
        } else if (lowerText.contains("ladka") || lowerText.contains("boy") || lowerText.contains("male") || lowerText.contains("groom")) {
            gender = "MALE"
        }

        // Age detection (e.g., "25 saal", "25-28", "25 year")
        val agePattern = Pattern.compile("(\\d{2})\\s*(saal|year|yr|)-?\\s*(\\d{2})?\\s*(saal|year|yr)?")
        val matcher = agePattern.matcher(lowerText)
        if (matcher.find()) {
            val a1 = matcher.group(1)?.toIntOrNull()
            val a2 = matcher.group(3)?.toIntOrNull()
            if (a1 != null && a1 in 18..70) {
                if (a2 != null && a2 in 18..70) {
                    minAge = minOf(a1, a2)
                    maxAge = maxOf(a1, a2)
                } else {
                    minAge = maxOf(18, a1 - 2)
                    maxAge = a1 + 2
                }
            }
        }

        // Cities
        val knownCities = listOf("gorakhpur", "lucknow", "varanasi", "kanpur", "delhi", "noida", "agra", "patna", "jaipur")
        knownCities.forEach { city ->
            if (lowerText.contains(city)) {
                val cityCap = city.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                if (!cities.contains(cityCap, ignoreCase = true)) {
                    cities = if (cities.isBlank()) cityCap else "$cities, $cityCap"
                }
            }
        }

        // Education
        val knownEdu = listOf("b.tech", "btech", "mba", "mbbs", "b.com", "m.tech", "bca", "mca")
        knownEdu.forEach { edu ->
            if (lowerText.contains(edu)) {
                if (!education.contains(edu, ignoreCase = true)) {
                    education = if (education.isBlank()) edu.uppercase() else "$education, ${edu.uppercase()}"
                }
            }
        }

        val rawAccumulated = if (existing.rawRequirementText.isBlank()) text else "${existing.rawRequirementText} | $text"

        val updatedReq = existing.copy(
            targetGender = gender,
            minAge = minAge,
            maxAge = maxAge,
            preferredCities = cities,
            preferredEducation = education,
            rawRequirementText = rawAccumulated,
            updatedAt = System.currentTimeMillis()
        )

        customerDao.insertOrUpdateRequirement(updatedReq)
        return updatedReq
    }

    override suspend fun addShownProfileCode(customerId: Long, profileCode: String) {
        val req = customerDao.getRequirementByCustomerId(customerId)
            ?: CustomerRequirementEntity(customerId = customerId)

        val existingCodes = req.shownProfileCodes.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        existingCodes.add(profileCode)

        val updated = req.copy(
            shownProfileCodes = existingCodes.joinToString(","),
            updatedAt = System.currentTimeMillis()
        )
        customerDao.insertOrUpdateRequirement(updated)
    }

    override suspend fun setHandoffStatus(customerId: Long, status: HandoffStatus) {
        customerDao.updateHandoffStatus(customerId, status)
    }

    override fun getAllCustomersFlow(): Flow<List<CustomerEntity>> {
        return customerDao.getAllCustomersFlow()
    }

    override fun getHumanRequiredCustomersFlow(): Flow<List<CustomerEntity>> {
        return customerDao.getHumanRequiredCustomersFlow()
    }

    override suspend fun clearHistory(customerId: Long) {
        customerDao.clearCustomerMessages(customerId)
    }
}
