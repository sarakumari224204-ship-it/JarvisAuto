package com.marriagebureau.ai.ai

import com.marriagebureau.ai.data.db.dao.CustomerDao
import com.marriagebureau.ai.data.db.dao.ProfileDao
import com.marriagebureau.ai.data.db.entity.*
import com.marriagebureau.ai.data.repository.CustomerRepositoryImpl
import com.marriagebureau.ai.data.repository.ProfileRepositoryImpl
import com.marriagebureau.ai.knowledge.dao.KnowledgeDao
import com.marriagebureau.ai.knowledge.entity.KnowledgeChunkEntity
import com.marriagebureau.ai.knowledge.entity.KnowledgeDocumentEntity
import com.marriagebureau.ai.knowledge.entity.WorkflowStepEntity
import com.marriagebureau.ai.knowledge.repository.KnowledgeRepositoryImpl
import com.marriagebureau.ai.teach.dao.TeachDao
import com.marriagebureau.ai.teach.entity.*
import com.marriagebureau.ai.teach.repository.TeachRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LocalLLMAndRAGTest {

    private class MockProfileDao : ProfileDao {
        val list = mutableListOf<MarriageProfileEntity>()
        override suspend fun insertProfile(profile: MarriageProfileEntity): Long { list.add(profile); return 1L }
        override suspend fun insertProfiles(profiles: List<MarriageProfileEntity>): List<Long> { list.addAll(profiles); return profiles.map { 1L } }
        override suspend fun updateProfile(profile: MarriageProfileEntity) {}
        override suspend fun getProfileById(id: Long): MarriageProfileEntity? = list.firstOrNull { it.profileId == id }
        override suspend fun getProfileByCode(code: String): MarriageProfileEntity? = list.firstOrNull { it.profileCode == code }
        override suspend fun getActiveProfilesPaged(limit: Int, offset: Int): List<MarriageProfileEntity> = list
        override fun getAllActiveProfilesFlow(): Flow<List<MarriageProfileEntity>> = flowOf(list)
        override suspend fun getActiveProfileCount(): Int = list.size
        override suspend fun getTotalProfileCount(): Int = list.size
        override suspend fun searchProfiles(
            gender: String?, minAge: Int?, maxAge: Int?, city: String?, maritalStatus: String?, education: String?, occupation: String?, limit: Int, offset: Int
        ): List<MarriageProfileEntity> {
            return list.filter { p ->
                (gender == null || p.gender.equals(gender, ignoreCase = true)) &&
                (city == null || p.city.contains(city, ignoreCase = true))
            }
        }
        override suspend fun freeTextSearch(query: String, limit: Int, offset: Int): List<MarriageProfileEntity> = list
        override suspend fun findDuplicates(code: String, name: String, city: String, age: Int, contact: String?): List<MarriageProfileEntity> = emptyList()
        override suspend fun updateProfileStatus(profileId: Long, status: ProfileStatus, updatedAt: Long) {}
        override suspend fun deleteProfile(profileId: Long) {}
        override suspend fun deleteAllProfiles() {}
    }

    private class MockCustomerDao : CustomerDao {
        val cust = CustomerEntity(customerId = 1L, whatsappIdentifier = "+919876543210", displayName = "Ramesh")
        val req = CustomerRequirementEntity(customerId = 1L, targetGender = "FEMALE", preferredCities = "Gorakhpur")
        val msgs = mutableListOf<ConversationMessageEntity>()

        override suspend fun insertCustomer(customer: CustomerEntity): Long = 1L
        override suspend fun updateCustomer(customer: CustomerEntity) {}
        override suspend fun getCustomerById(id: Long): CustomerEntity = cust
        override suspend fun getCustomerByWhatsappId(identifier: String): CustomerEntity = cust
        override fun getAllCustomersFlow(): Flow<List<CustomerEntity>> = flowOf(listOf(cust))
        override fun getHumanRequiredCustomersFlow(): Flow<List<CustomerEntity>> = flowOf(emptyList())
        override suspend fun insertMessage(message: ConversationMessageEntity): Long { msgs.add(message); return 1L }
        override suspend fun getMessagesForCustomer(customerId: Long, limit: Int, offset: Int): List<ConversationMessageEntity> = msgs
        override suspend fun getRecentMessages(customerId: Long, limit: Int): List<ConversationMessageEntity> = msgs
        override suspend fun insertOrUpdateRequirement(requirement: CustomerRequirementEntity): Long = 1L
        override suspend fun getRequirementByCustomerId(customerId: Long): CustomerRequirementEntity = req
        override suspend fun updateHandoffStatus(customerId: Long, status: HandoffStatus, timestamp: Long) {}
        override suspend fun clearCustomerMessages(customerId: Long) {}
        override suspend fun deleteCustomer(customerId: Long) {}
    }

    private class MockTeachDao : TeachDao {
        override suspend fun saveIdentity(identity: AgentIdentityEntity) {}
        override suspend fun getIdentity(): AgentIdentityEntity = AgentIdentityEntity()
        override suspend fun insertInstruction(instruction: InstructionEntity): Long = 1L
        override suspend fun updateInstruction(instruction: InstructionEntity) {}
        override suspend fun getActiveInstructions(): List<InstructionEntity> = listOf(InstructionEntity(title = "Strict", instruction = "Do not invent profile info"))
        override fun getAllInstructionsFlow(): Flow<List<InstructionEntity>> = flowOf(emptyList())
        override suspend fun getAllInstructions(): List<InstructionEntity> = emptyList()
        override suspend fun deleteInstruction(id: Long) {}
        override suspend fun deleteAllInstructions() {}
        override suspend fun insertRule(rule: RuleEntity): Long = 1L
        override suspend fun updateRule(rule: RuleEntity) {}
        override suspend fun getActiveRules(): List<RuleEntity> = listOf(RuleEntity(title = "Verify", ruleCondition = "If match exists", actionText = "State confirmed profiles only"))
        override fun getAllRulesFlow(): Flow<List<RuleEntity>> = flowOf(emptyList())
        override suspend fun getAllRules(): List<RuleEntity> = emptyList()
        override suspend fun deleteRule(id: Long) {}
        override suspend fun deleteAllRules() {}
        override suspend fun insertExample(example: ExampleConversationEntity): Long = 1L
        override suspend fun getActiveExamples(): List<ExampleConversationEntity> = emptyList()
        override suspend fun getExamplesByCategory(category: ExampleCategory): List<ExampleConversationEntity> = emptyList()
        override fun getAllExamplesFlow(): Flow<List<ExampleConversationEntity>> = flowOf(emptyList())
        override suspend fun getAllExamples(): List<ExampleConversationEntity> = emptyList()
        override suspend fun deleteExample(id: Long) {}
        override suspend fun deleteAllExamples() {}
        override suspend fun insertVersion(version: TrainingVersionEntity): Long = 1L
        override fun getAllVersionsFlow(): Flow<List<TrainingVersionEntity>> = flowOf(emptyList())
        override suspend fun getVersionById(versionId: Long): TrainingVersionEntity? = null
        override suspend fun deactivateAllVersions() {}
        override suspend fun activateVersion(versionId: Long) {}
    }

    private class MockKnowledgeDao : KnowledgeDao {
        override suspend fun insertDocument(document: KnowledgeDocumentEntity): Long = 1L
        override fun getAllDocumentsFlow(): Flow<List<KnowledgeDocumentEntity>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: Long): KnowledgeDocumentEntity? = null
        override suspend fun deleteDocument(id: Long) {}
        override suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>): List<Long> = listOf(1L)
        override suspend fun getChunksForDocument(documentId: Long): List<KnowledgeChunkEntity> = emptyList()
        override suspend fun searchChunks(query: String, limit: Int): List<KnowledgeChunkEntity> = listOf(KnowledgeChunkEntity(documentId = 1L, chunkIndex = 0, chunkText = "Office registration fee Rs 1000."))
        override suspend fun deleteChunksForDocument(documentId: Long) {}
        override suspend fun insertWorkflowStep(step: WorkflowStepEntity): Long = 1L
        override suspend fun getWorkflowSteps(workflowName: String): List<WorkflowStepEntity> = emptyList()
        override suspend fun clearWorkflow(workflowName: String) {}
    }

    private lateinit var profileRepo: ProfileRepositoryImpl
    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var teachRepo: TeachRepositoryImpl
    private lateinit var knowledgeRepo: KnowledgeRepositoryImpl
    private lateinit var llmEngine: FallbackTemplateLLMEngine
    private lateinit var ragPipeline: RAGPipeline
    private lateinit var validator: ResponseValidator

    @Before
    fun setUp() = runBlocking {
        val profileDao = MockProfileDao()
        profileDao.insertProfile(
            MarriageProfileEntity(profileId = 101L, profileCode = "MB101", name = "Priya Sharma", gender = "FEMALE", age = 25, city = "Gorakhpur", education = "B.Tech")
        )

        profileRepo = ProfileRepositoryImpl(profileDao)
        customerRepo = CustomerRepositoryImpl(MockCustomerDao())
        teachRepo = TeachRepositoryImpl(MockTeachDao())
        knowledgeRepo = KnowledgeRepositoryImpl(MockKnowledgeDao())
        llmEngine = FallbackTemplateLLMEngine()
        ragPipeline = RAGPipeline(profileRepo, customerRepo, knowledgeRepo, teachRepo, llmEngine)
        validator = ResponseValidator()
    }

    @Test
    fun testRAGPipelineAndGeneration() = runBlocking {
        val (context, response) = ragPipeline.buildContextAndGenerate(1L, "Gorakhpur mein 25 saal ki ladki ke profiles hain?")

        assertNotNull(context)
        assertEquals(1, context.matchedProfiles.size)
        assertEquals("MB101", context.matchedProfiles[0].profileCode)
        assertTrue(response.contains("MB101") || response.contains("verified profiles"))
    }

    @Test
    fun testResponseValidatorHallucinationBlocking() {
        val matched = listOf(
            MarriageProfileEntity(profileCode = "MB101", name = "Priya", gender = "FEMALE", age = 25, city = "Gorakhpur")
        )

        // Valid response citing matched code MB101
        val validRes = validator.validateAndSanitize("Aapki requirement ke according MB101 profile available hai.", matched)
        assertTrue(validRes.isValid)
        assertFalse(validRes.isHallucinationBlocked)

        // Invalid response citing fake code MB999
        val invalidRes = validator.validateAndSanitize("Aap MB999 profile check karein.", matched)
        assertFalse(invalidRes.isValid)
        assertTrue(invalidRes.isHallucinationBlocked)
        assertTrue(invalidRes.sanitizedResponse.contains("active verified profiles database mein check kar ke"))
    }
}
