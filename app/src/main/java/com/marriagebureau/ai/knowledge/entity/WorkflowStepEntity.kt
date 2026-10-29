package com.marriagebureau.ai.knowledge.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflow_steps")
data class WorkflowStepEntity(
    @PrimaryKey(autoGenerate = true)
    val stepId: Long = 0,
    val workflowName: String = "DEFAULT_PROFILE_REQUEST",
    val stepOrder: Int,
    val title: String,
    val description: String,
    val requiresApproval: Boolean = false,
    val enabled: Boolean = true
)
