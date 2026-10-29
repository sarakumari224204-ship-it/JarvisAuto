package com.marriagebureau.ai.teach.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teach_instructions",
    indices = [Index(value = ["priority"])]
)
data class InstructionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val instruction: String,
    val priority: Int = 1, // Higher priority applied first
    val enabled: Boolean = true,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
