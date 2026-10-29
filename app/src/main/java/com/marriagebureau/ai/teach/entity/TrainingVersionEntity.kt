package com.marriagebureau.ai.teach.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_versions")
data class TrainingVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val versionId: Long = 0,
    val versionName: String, // e.g., "Training Version 1"
    val description: String = "",
    val identityJson: String,
    val instructionsJson: String,
    val rulesJson: String,
    val examplesJson: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
