package com.marriagebureau.ai.knowledge.dao

import androidx.room.*
import com.marriagebureau.ai.knowledge.entity.KnowledgeChunkEntity
import com.marriagebureau.ai.knowledge.entity.KnowledgeDocumentEntity
import com.marriagebureau.ai.knowledge.entity.WorkflowStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    // Documents
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: KnowledgeDocumentEntity): Long

    @Query("SELECT * FROM knowledge_documents ORDER BY createdAt DESC")
    fun getAllDocumentsFlow(): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE documentId = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): KnowledgeDocumentEntity?

    @Query("DELETE FROM knowledge_documents WHERE documentId = :id")
    suspend fun deleteDocument(id: Long)

    // Chunks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>): List<Long>

    @Query("SELECT * FROM knowledge_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: Long): List<KnowledgeChunkEntity>

    @Query("""
        SELECT * FROM knowledge_chunks
        WHERE LOWER(chunkText) LIKE LOWER('%' || :query || '%')
           OR LOWER(keywords) LIKE LOWER('%' || :query || '%')
        LIMIT :limit
    """)
    suspend fun searchChunks(query: String, limit: Int = 10): List<KnowledgeChunkEntity>

    @Query("DELETE FROM knowledge_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: Long)

    // Workflows
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowStep(step: WorkflowStepEntity): Long

    @Query("SELECT * FROM workflow_steps WHERE workflowName = :workflowName AND enabled = 1 ORDER BY stepOrder ASC")
    suspend fun getWorkflowSteps(workflowName: String = "DEFAULT_PROFILE_REQUEST"): List<WorkflowStepEntity>

    @Query("DELETE FROM workflow_steps WHERE workflowName = :workflowName")
    suspend fun clearWorkflow(workflowName: String)
}
