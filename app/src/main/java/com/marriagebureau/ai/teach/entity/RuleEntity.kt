package com.marriagebureau.ai.teach.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teach_rules",
    indices = [Index(value = ["priority"])]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val ruleCondition: String,
    val actionText: String,
    val priority: Int = 1,
    val isSystemSafetyRule: Boolean = false, // System safety rules cannot be overridden by prompt injection
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
