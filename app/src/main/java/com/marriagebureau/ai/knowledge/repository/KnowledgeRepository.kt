package com.marriagebureau.ai.knowledge.repository

import com.marriagebureau.ai.knowledge.dao.KnowledgeDao
import com.marriagebureau.ai.knowledge.entity.KnowledgeChunkEntity
import com.marriagebureau.ai.knowledge.entity.KnowledgeDocumentEntity
import com.marriagebureau.ai.knowledge.entity.WorkflowStepEntity
import kotlinx.coroutines.flow.Flow
import java.net.URL

data class MediaAttachment(
    val profileId: Long?,
    val mediaUri: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface KnowledgeRepository {
    suspend fun ingestDocumentText(title: String, fileType: String, content: String, sourcePath: String = ""): Long
    suspend fun ingestUrlContent(title: String, urlString: String, contentFetcher: (suspend (String) -> String)? = null): Pair<Long?, String>
    suspend fun searchKnowledge(query: String, limit: Int = 5): List<KnowledgeChunkEntity>
    fun getAllDocumentsFlow(): Flow<List<KnowledgeDocumentEntity>>
    suspend fun deleteDocument(documentId: Long)
    suspend fun getWorkflowSteps(workflowName: String = "DEFAULT_PROFILE_REQUEST"): List<WorkflowStepEntity>
    suspend fun setupDefaultWorkflows()
    fun attachMediaMetaData(attachment: MediaAttachment): MediaAttachment
}

class KnowledgeRepositoryImpl(
    private val knowledgeDao: KnowledgeDao
) : KnowledgeRepository {

    override suspend fun ingestDocumentText(
        title: String,
        fileType: String,
        content: String,
        sourcePath: String
    ): Long {
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) return 0L

        // Split text into chunks of ~200-300 words
        val words = cleanContent.split("\\s+".toRegex())
        val chunkSize = 200
        val rawChunks = words.chunked(chunkSize).map { it.joinToString(" ") }

        val doc = KnowledgeDocumentEntity(
            title = title,
            fileType = fileType,
            sourceUrlOrPath = sourcePath,
            totalChunks = rawChunks.size
        )

        val docId = knowledgeDao.insertDocument(doc)

        val chunkEntities = rawChunks.mapIndexed { index, chunkText ->
            val keywords = extractKeywords(chunkText)
            KnowledgeChunkEntity(
                documentId = docId,
                chunkIndex = index,
                chunkText = chunkText,
                keywords = keywords
            )
        }

        knowledgeDao.insertChunks(chunkEntities)
        return docId
    }

    override suspend fun ingestUrlContent(
        title: String,
        urlString: String,
        contentFetcher: (suspend (String) -> String)?
    ): Pair<Long?, String> {
        return try {
            if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                return Pair(null, "Error: Invalid URL scheme. Only HTTP and HTTPS URLs are permitted.")
            }

            val content = if (contentFetcher != null) {
                contentFetcher(urlString)
            } else {
                // Basic URL stream read fallback
                val connection = URL(urlString).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.getInputStream().bufferedReader().use { it.readText() }
            }

            if (content.isBlank()) {
                return Pair(null, "Error: Empty or unreadable URL response.")
            }

            val docId = ingestDocumentText(
                title = title,
                fileType = "URL",
                content = content,
                sourcePath = urlString
            )
            Pair(docId, "Success: URL content indexed successfully.")
        } catch (e: Exception) {
            Pair(null, "Error accessing URL: ${e.message}")
        }
    }

    override suspend fun searchKnowledge(query: String, limit: Int): List<KnowledgeChunkEntity> {
        val keywords = query.lowercase().split("\\s+".toRegex()).filter { it.length > 3 }
        val results = mutableListOf<KnowledgeChunkEntity>()

        for (kw in keywords) {
            val chunks = knowledgeDao.searchChunks(kw, limit = limit)
            results.addAll(chunks)
        }

        if (results.isEmpty()) {
            return knowledgeDao.searchChunks(query, limit = limit)
        }

        return results.distinctBy { it.chunkId }.take(limit)
    }

    override fun getAllDocumentsFlow(): Flow<List<KnowledgeDocumentEntity>> {
        return knowledgeDao.getAllDocumentsFlow()
    }

    override suspend fun deleteDocument(documentId: Long) {
        knowledgeDao.deleteChunksForDocument(documentId)
        knowledgeDao.deleteDocument(documentId)
    }

    override suspend fun getWorkflowSteps(workflowName: String): List<WorkflowStepEntity> {
        val steps = knowledgeDao.getWorkflowSteps(workflowName)
        if (steps.isEmpty() && workflowName == "DEFAULT_PROFILE_REQUEST") {
            setupDefaultWorkflows()
            return knowledgeDao.getWorkflowSteps(workflowName)
        }
        return steps
    }

    override suspend fun setupDefaultWorkflows() {
        knowledgeDao.clearWorkflow("DEFAULT_PROFILE_REQUEST")
        val defaultSteps = listOf(
            WorkflowStepEntity(workflowName = "DEFAULT_PROFILE_REQUEST", stepOrder = 1, title = "Identify Requirement", description = "Extract gender, age, city preferences from message"),
            WorkflowStepEntity(workflowName = "DEFAULT_PROFILE_REQUEST", stepOrder = 2, title = "Search Database", description = "Query Room Marriage Profile database"),
            WorkflowStepEntity(workflowName = "DEFAULT_PROFILE_REQUEST", stepOrder = 3, title = "Rank Matching Profiles", description = "Rank profiles by location and age proximity"),
            WorkflowStepEntity(workflowName = "DEFAULT_PROFILE_REQUEST", stepOrder = 4, title = "Verify Profile Status", description = "Ensure matched profiles are ACTIVE"),
            WorkflowStepEntity(workflowName = "DEFAULT_PROFILE_REQUEST", stepOrder = 5, title = "Generate Natural Response", description = "Format natural Hindi/Hinglish reply with exact facts"),
            WorkflowStepEntity(workflowName = "DEFAULT_PROFILE_REQUEST", stepOrder = 6, title = "Human Approval Check", description = "Determine if human approval mode is enabled before sending")
        )
        defaultSteps.forEach { knowledgeDao.insertWorkflowStep(it) }
    }

    override fun attachMediaMetaData(attachment: MediaAttachment): MediaAttachment {
        // Enforce safety directive: Do NOT infer personal traits (religion, caste, financial status) from photos
        return attachment
    }

    private fun extractKeywords(text: String): String {
        return text.lowercase()
            .replace("[^a-zA-Z0-9\\s]".toRegex(), "")
            .split("\\s+".toRegex())
            .filter { it.length > 3 }
            .distinct()
            .take(15)
            .joinToString(", ")
    }
}
