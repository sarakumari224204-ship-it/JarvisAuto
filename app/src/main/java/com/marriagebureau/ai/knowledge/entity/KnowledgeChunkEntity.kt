package com.marriagebureau.ai.knowledge.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_chunks",
    indices = [
        Index(value = ["documentId"])
    ]
)
data class KnowledgeChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val chunkId: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val chunkText: String,
    val keywords: String = "" // Space/comma-separated extracted keywords
)
