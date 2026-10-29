package com.marriagebureau.ai.teach.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_identity")
data class AgentIdentityEntity(
    @PrimaryKey
    val id: Int = 1, // Single row configuration
    val agentName: String = "Sara",
    val bureauName: String = "Royal Marriage Bureau",
    val ownerName: String = "Manager",
    val role: String = "Senior Marriage Assistant",
    val languages: String = "Hindi, Hinglish, English",
    val tone: String = "Polite, Professional, Respectful",
    val workingHours: String = "9 AM - 8 PM",
    val generalBehavior: String = "Polite, helpful, concise, never invent profile facts.",
    val updatedAt: Long = System.currentTimeMillis()
)
