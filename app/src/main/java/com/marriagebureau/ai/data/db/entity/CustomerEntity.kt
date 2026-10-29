package com.marriagebureau.ai.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HandoffStatus {
    AUTOMATED,
    HUMAN_REQUIRED
}

enum class CustomerMemoryStatus {
    ACTIVE,
    PAUSED,
    ARCHIVED
}

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["whatsappIdentifier"], unique = true),
        Index(value = ["handoffStatus"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val customerId: Long = 0,
    val whatsappIdentifier: String, // Phone or JID
    val displayName: String = "",
    val languagePreference: String = "HINGLISH", // HINDI, HINGLISH, ENGLISH
    val memoryStatus: CustomerMemoryStatus = CustomerMemoryStatus.ACTIVE,
    val handoffStatus: HandoffStatus = HandoffStatus.AUTOMATED,
    val notes: String = "",
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
