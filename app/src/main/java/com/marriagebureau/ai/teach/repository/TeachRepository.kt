package com.marriagebureau.ai.teach.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marriagebureau.ai.teach.dao.TeachDao
import com.marriagebureau.ai.teach.entity.*
import kotlinx.coroutines.flow.Flow

interface TeachRepository {
    suspend fun getIdentity(): AgentIdentityEntity
    suspend fun saveIdentity(identity: AgentIdentityEntity)

    suspend fun addInstruction(instruction: InstructionEntity): Long
    suspend fun getActiveInstructions(): List<InstructionEntity>
    fun getAllInstructionsFlow(): Flow<List<InstructionEntity>>
    suspend fun deleteInstruction(id: Long)

    suspend fun addRule(rule: RuleEntity): Long
    suspend fun getActiveRules(): List<RuleEntity>
    fun getAllRulesFlow(): Flow<List<RuleEntity>>
    suspend fun deleteRule(id: Long)

    suspend fun addExample(example: ExampleConversationEntity): Long
    suspend fun getActiveExamples(): List<ExampleConversationEntity>
    fun getAllExamplesFlow(): Flow<List<ExampleConversationEntity>>
    suspend fun deleteExample(id: Long)

    suspend fun recordTeachCorrection(
        customerMessage: String,
        wrongResponse: String,
        correctResponse: String,
        category: ExampleCategory = ExampleCategory.PROFILE_SEARCH
    )

    suspend fun createTrainingVersionSnapshot(versionName: String, description: String = ""): TrainingVersionEntity
    suspend fun rollbackToVersion(versionId: Long): Boolean
    fun getAllVersionsFlow(): Flow<List<TrainingVersionEntity>>
}

class TeachRepositoryImpl(
    private val teachDao: TeachDao,
    private val gson: Gson = Gson()
) : TeachRepository {

    override suspend fun getIdentity(): AgentIdentityEntity {
        return teachDao.getIdentity() ?: AgentIdentityEntity().also {
            teachDao.saveIdentity(it)
        }
    }

    override suspend fun saveIdentity(identity: AgentIdentityEntity) {
        teachDao.saveIdentity(identity)
    }

    override suspend fun addInstruction(instruction: InstructionEntity): Long {
        return teachDao.insertInstruction(instruction)
    }

    override suspend fun getActiveInstructions(): List<InstructionEntity> {
        return teachDao.getActiveInstructions()
    }

    override fun getAllInstructionsFlow(): Flow<List<InstructionEntity>> {
        return teachDao.getAllInstructionsFlow()
    }

    override suspend fun deleteInstruction(id: Long) {
        teachDao.deleteInstruction(id)
    }

    override suspend fun addRule(rule: RuleEntity): Long {
        return teachDao.insertRule(rule)
    }

    override suspend fun getActiveRules(): List<RuleEntity> {
        return teachDao.getActiveRules()
    }

    override fun getAllRulesFlow(): Flow<List<RuleEntity>> {
        return teachDao.getAllRulesFlow()
    }

    override suspend fun deleteRule(id: Long) {
        teachDao.deleteRule(id)
    }

    override suspend fun addExample(example: ExampleConversationEntity): Long {
        return teachDao.insertExample(example)
    }

    override suspend fun getActiveExamples(): List<ExampleConversationEntity> {
        return teachDao.getActiveExamples()
    }

    override fun getAllExamplesFlow(): Flow<List<ExampleConversationEntity>> {
        return teachDao.getAllExamplesFlow()
    }

    override suspend fun deleteExample(id: Long) {
        teachDao.deleteExample(id)
    }

    override suspend fun recordTeachCorrection(
        customerMessage: String,
        wrongResponse: String,
        correctResponse: String,
        category: ExampleCategory
    ) {
        val example = ExampleConversationEntity(
            category = category,
            customerMessage = customerMessage,
            idealResponse = correctResponse,
            contextNote = "Correction added by owner for previous wrong response: '$wrongResponse'",
            enabled = true
        )
        teachDao.insertExample(example)

        val instruction = InstructionEntity(
            title = "Correction for: ${customerMessage.take(20)}...",
            instruction = "When customer says '$customerMessage', respond with: '$correctResponse'",
            priority = 10,
            enabled = true
        )
        teachDao.insertInstruction(instruction)
    }

    override suspend fun createTrainingVersionSnapshot(
        versionName: String,
        description: String
    ): TrainingVersionEntity {
        val identity = getIdentity()
        val instructions = teachDao.getAllInstructions()
        val rules = teachDao.getAllRules()
        val examples = teachDao.getAllExamples()

        val version = TrainingVersionEntity(
            versionName = versionName,
            description = description,
            identityJson = gson.toJson(identity),
            instructionsJson = gson.toJson(instructions),
            rulesJson = gson.toJson(rules),
            examplesJson = gson.toJson(examples),
            isActive = true
        )

        teachDao.deactivateAllVersions()
        val id = teachDao.insertVersion(version)
        return version.copy(versionId = id)
    }

    override suspend fun rollbackToVersion(versionId: Long): Boolean {
        val version = teachDao.getVersionById(versionId) ?: return false

        return try {
            val identityType = object : TypeToken<AgentIdentityEntity>() {}.type
            val instructionsType = object : TypeToken<List<InstructionEntity>>() {}.type
            val rulesType = object : TypeToken<List<RuleEntity>>() {}.type
            val examplesType = object : TypeToken<List<ExampleConversationEntity>>() {}.type

            val identity: AgentIdentityEntity = gson.fromJson(version.identityJson, identityType)
            val instructions: List<InstructionEntity> = gson.fromJson(version.instructionsJson, instructionsType)
            val rules: List<RuleEntity> = gson.fromJson(version.rulesJson, rulesType)
            val examples: List<ExampleConversationEntity> = gson.fromJson(version.examplesJson, examplesType)

            teachDao.saveIdentity(identity)

            teachDao.deleteAllInstructions()
            instructions.forEach { teachDao.insertInstruction(it.copy(id = 0)) }

            teachDao.deleteAllRules()
            rules.forEach { teachDao.insertRule(it.copy(id = 0)) }

            teachDao.deleteAllExamples()
            examples.forEach { teachDao.insertExample(it.copy(id = 0)) }

            teachDao.deactivateAllVersions()
            teachDao.activateVersion(versionId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAllVersionsFlow(): Flow<List<TrainingVersionEntity>> {
        return teachDao.getAllVersionsFlow()
    }
}
