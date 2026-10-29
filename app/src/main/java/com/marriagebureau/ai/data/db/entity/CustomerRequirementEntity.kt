package com.marriagebureau.ai.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_requirements",
    indices = [
        Index(value = ["customerId"], unique = true)
    ]
)
data class CustomerRequirementEntity(
    @PrimaryKey(autoGenerate = true)
    val requirementId: Long = 0,
    val customerId: Long,
    val targetGender: String? = null, // "FEMALE", "MALE"
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val preferredCities: String = "", // Comma-separated cities
    val preferredEducation: String = "",
    val preferredOccupation: String = "",
    val maritalStatus: String? = null,
    val rawRequirementText: String = "",
    val shownProfileCodes: String = "", // Comma-separated list of profile codes shown
    val updatedAt: Long = System.currentTimeMillis()
)
