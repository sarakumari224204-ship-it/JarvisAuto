package com.marriagebureau.ai.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProfileStatus {
    ACTIVE,
    ARCHIVED,
    DELETED
}

@Entity(
    tableName = "marriage_profiles",
    indices = [
        Index(value = ["profileCode"], unique = true),
        Index(value = ["gender"]),
        Index(value = ["age"]),
        Index(value = ["city"]),
        Index(value = ["maritalStatus"]),
        Index(value = ["education"]),
        Index(value = ["occupation"]),
        Index(value = ["status"]),
        Index(value = ["gender", "status", "age"])
    ]
)
data class MarriageProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val profileId: Long = 0,
    val profileCode: String,
    val name: String,
    val gender: String, // "MALE" or "FEMALE"
    val age: Int,
    val dateOfBirth: String? = null,
    val city: String,
    val state: String = "",
    val education: String = "",
    val occupation: String = "",
    val incomeRange: String? = null,
    val maritalStatus: String = "NEVER_MARRIED", // "NEVER_MARRIED", "DIVORCED", "WIDOWED"
    val familyInformation: String? = null,
    val preferences: String? = null,
    val contactInformation: String? = null,
    val profileDescription: String = "",
    val photoReferences: String = "", // Comma-separated or JSON list of URIs/paths
    val source: String = "MANUAL", // CSV, EXCEL, PDF, MANUAL
    val status: ProfileStatus = ProfileStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
