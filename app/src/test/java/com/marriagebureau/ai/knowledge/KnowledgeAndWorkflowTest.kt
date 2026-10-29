package com.marriagebureau.ai.knowledge

import com.marriagebureau.ai.knowledge.dao.KnowledgeDao
import com.marriagebureau.ai.knowledge.entity.KnowledgeChunkEntity
import com.marriagebureau.ai.knowledge.entity.KnowledgeDocumentEntity
import com.marriagebureau.ai.knowledge.entity.WorkflowStepEntity
import com.marriagebureau.ai.knowledge.repository.KnowledgeRepositoryImpl
import com.marriagebureau.ai.knowledge.repository.MediaAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KnowledgeAndWorkflowTest {

    private class InMemKnowledgeDao : KnowledgeDao {
        val docs = mutableMapOf<Long, KnowledgeDocumentEntity>()
        val chunksList = mutableListOf<KnowledgeChunkEntity>()
        val workflows = mutableListOf<WorkflowStepEntity>()

        var docIdGen = 1L
        var chunkIdGen = 1L
        var wfIdGen = 1L

        override suspend fun insertDocument(document: KnowledgeDocumentEntity): Long {
            val id = if (document.documentId == 0L) docIdGen++ else document.documentId
            docs[id] = document.copy(documentId = id)
            return id
        }

        override fun getAllDocumentsFlow(): Flow<List<KnowledgeDocumentEntity>> = flowOf(docs.values.toList())

        override suspend fun getDocumentById(id: Long): KnowledgeDocumentEntity? = docs[id]

        override suspend fun deleteDocument(id: Long) {
            docs.remove(id)
        }

        override suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>): List<Long> {
            return chunks.map {
                val id = if (it.chunkId == 0L) chunkIdGen++ else it.chunkId
                val saved = it.copy(chunkId = id)
                chunksList.add(saved)
                id
            }
        }

        override suspend fun getChunksForDocument(documentId: Long): List<KnowledgeChunkEntity> {
            return chunksList.filter { it.documentId == documentId }
        }

        override suspend fun searchChunks(query: String, limit: Int): List<KnowledgeChunkEntity> {
            val q = query.lowercase()
            return chunksList.filter {
                it.chunkText.lowercase().contains(q) || it.keywords.lowercase().contains(q)
            }.take(limit)
        }

        override suspend fun deleteChunksForDocument(documentId: Long) {
            chunksList.removeAll { it.documentId == documentId }
        }

        override suspend fun insertWorkflowStep(step: WorkflowStepEntity): Long {
            val id = if (step.stepId == 0L) wfIdGen++ else step.stepId
            val saved = step.copy(stepId = id)
            workflows.add(saved)
            return id
        }

        override suspend fun getWorkflowSteps(workflowName: String): List<WorkflowStepEntity> {
            return workflows.filter { it.workflowName == workflowName && it.enabled }.sortedBy { it.stepOrder }
        }

        override suspend fun clearWorkflow(workflowName: String) {
            workflows.removeAll { it.workflowName == workflowName }
        }
    }

    private lateinit var dao: InMemKnowledgeDao
    private lateinit var repository: KnowledgeRepositoryImpl

    @Before
    fun setUp() {
        dao = InMemKnowledgeDao()
        repository = KnowledgeRepositoryImpl(dao)
    }

    @Test
    fun testTextDocumentIngestionAndSearch() = runBlocking {
        val sampleText = """
            Royal Marriage Bureau Rules and Fees:
            Registration fee is Rs 1000. Service duration is 1 year.
            Office hours are 9 AM to 8 PM Monday to Saturday.
            Gorakhpur office location is Civil Lines.
        """.trimIndent()

        val docId = repository.ingestDocumentText("Rules Document", "TXT", sampleText)
        assertTrue(docId > 0)

        val results = repository.searchKnowledge("Registration fee")
        assertFalse(results.isEmpty())
        assertTrue(results[0].chunkText.contains("Rs 1000"))
    }

    @Test
    fun testUrlContentImportHandling() = runBlocking {
        val (docId, message) = repository.ingestUrlContent(
            title = "Bureau Website FAQ",
            urlString = "https://example.com/faq",
            contentFetcher = { "Official FAQ: Registration fees, process, and profile verification steps." }
        )

        assertNotNull(docId)
        assertTrue(message.contains("Success"))

        val results = repository.searchKnowledge("verification")
        assertFalse(results.isEmpty())
    }

    @Test
    fun testDefaultWorkflowInitialization() = runBlocking {
        val steps = repository.getWorkflowSteps("DEFAULT_PROFILE_REQUEST")
        assertEquals(6, steps.size)
        assertEquals("Identify Requirement", steps[0].title)
    }

    @Test
    fun testMediaMetaDataAttachment() {
        val attachment = MediaAttachment(
            profileId = 101L,
            mediaUri = "file:///storage/emulated/0/Profiles/MB101.jpg",
            description = "Verified profile photo"
        )
        val processed = repository.attachMediaMetaData(attachment)
        assertEquals(101L, processed.profileId)
        assertEquals("Verified profile photo", processed.description)
    }
}
