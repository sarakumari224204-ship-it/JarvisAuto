package com.marriagebureau.ai.knowledge.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_documents")
data class KnowledgeDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val documentId: Long = 0,
    val title: String,
    val fileType: String = "TXT", // TXT, JSON, PDF, URL
    val sourceUrlOrPath: String = "",
    val totalChunks: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
