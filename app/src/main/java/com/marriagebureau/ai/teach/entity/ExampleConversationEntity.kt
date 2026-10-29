package com.marriagebureau.ai.teach.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExampleCategory {
    NEW_CUSTOMER,
    PROFILE_SEARCH,
    FOLLOW_UP,
    PRICE_FEE,
    PROFILE_REQUEST,
    PHOTO_REQUEST,
    CONTACT_REQUEST,
    COMPLAINT,
    GENERAL_INQUIRY,
    HUMAN_HANDOFF
}

@Entity(
    tableName = "example_conversations",
    indices = [Index(value = ["category"])]
)
data class ExampleConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: ExampleCategory = ExampleCategory.PROFILE_SEARCH,
    val customerMessage: String,
    val idealResponse: String,
    val contextNote: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
