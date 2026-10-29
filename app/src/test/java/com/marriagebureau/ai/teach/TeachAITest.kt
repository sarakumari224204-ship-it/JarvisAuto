package com.marriagebureau.ai.teach

import com.marriagebureau.ai.teach.dao.TeachDao
import com.marriagebureau.ai.teach.entity.*
import com.marriagebureau.ai.teach.repository.TeachRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TeachAITest {

    private class InMemTeachDao : TeachDao {
        var identity: AgentIdentityEntity? = null
        val instructions = mutableMapOf<Long, InstructionEntity>()
        val rules = mutableMapOf<Long, RuleEntity>()
        val examples = mutableMapOf<Long, ExampleConversationEntity>()
        val versions = mutableMapOf<Long, TrainingVersionEntity>()

        var instId = 1L
        var ruleId = 1L
        var exId = 1L
        var verId = 1L

        override suspend fun saveIdentity(identity: AgentIdentityEntity) {
            this.identity = identity
        }

        override suspend fun getIdentity(): AgentIdentityEntity? = identity

        override suspend fun insertInstruction(instruction: InstructionEntity): Long {
            val id = if (instruction.id == 0L) instId++ else instruction.id
            instructions[id] = instruction.copy(id = id)
            return id
        }

        override suspend fun updateInstruction(instruction: InstructionEntity) {
            instructions[instruction.id] = instruction
        }

        override suspend fun getActiveInstructions(): List<InstructionEntity> {
            return instructions.values.filter { it.enabled }.sortedByDescending { it.priority }
        }

        override fun getAllInstructionsFlow(): Flow<List<InstructionEntity>> = flowOf(instructions.values.toList())

        override suspend fun getAllInstructions(): List<InstructionEntity> = instructions.values.toList()

        override suspend fun deleteInstruction(id: Long) {
            instructions.remove(id)
        }

        override suspend fun deleteAllInstructions() {
            instructions.clear()
        }

        override suspend fun insertRule(rule: RuleEntity): Long {
            val id = if (rule.id == 0L) ruleId++ else rule.id
            rules[id] = rule.copy(id = id)
            return id
        }

        override suspend fun updateRule(rule: RuleEntity) {
            rules[rule.id] = rule
        }

        override suspend fun getActiveRules(): List<RuleEntity> {
            return rules.values.filter { it.enabled }.sortedByDescending { it.priority }
        }

        override fun getAllRulesFlow(): Flow<List<RuleEntity>> = flowOf(rules.values.toList())

        override suspend fun getAllRules(): List<RuleEntity> = rules.values.toList()

        override suspend fun deleteRule(id: Long) {
            rules.remove(id)
        }

        override suspend fun deleteAllRules() {
            rules.clear()
        }

        override suspend fun insertExample(example: ExampleConversationEntity): Long {
            val id = if (example.id == 0L) exId++ else example.id
            examples[id] = example.copy(id = id)
            return id
        }

        override suspend fun getActiveExamples(): List<ExampleConversationEntity> {
            return examples.values.filter { it.enabled }
        }

        override suspend fun getExamplesByCategory(category: ExampleCategory): List<ExampleConversationEntity> {
            return examples.values.filter { it.category == category && it.enabled }
        }

        override fun getAllExamplesFlow(): Flow<List<ExampleConversationEntity>> = flowOf(examples.values.toList())

        override suspend fun getAllExamples(): List<ExampleConversationEntity> = examples.values.toList()

        override suspend fun deleteExample(id: Long) {
            examples.remove(id)
        }

        override suspend fun deleteAllExamples() {
            examples.clear()
        }

        override suspend fun insertVersion(version: TrainingVersionEntity): Long {
            val id = if (version.versionId == 0L) verId++ else version.versionId
            versions[id] = version.copy(versionId = id)
            return id
        }

        override fun getAllVersionsFlow(): Flow<List<TrainingVersionEntity>> = flowOf(versions.values.toList())

        override suspend fun getVersionById(versionId: Long): TrainingVersionEntity? = versions[versionId]

        override suspend fun deactivateAllVersions() {
            versions.keys.forEach { id ->
                versions[id] = versions[id]!!.copy(isActive = false)
            }
        }

        override suspend fun activateVersion(versionId: Long) {
            versions[versionId]?.let {
                versions[versionId] = it.copy(isActive = true)
            }
        }
    }

    private lateinit var dao: InMemTeachDao
    private lateinit var repository: TeachRepositoryImpl

    @Before
    fun setUp() {
        dao = InMemTeachDao()
        repository = TeachRepositoryImpl(dao)
    }

    @Test
    fun testIdentityAndInstructions() = runBlocking {
        repository.saveIdentity(AgentIdentityEntity(agentName = "Priya Agent", bureauName = "Gorakhpur Bureau"))
        val identity = repository.getIdentity()
        assertEquals("Priya Agent", identity.agentName)

        repository.addInstruction(InstructionEntity(title = "No Inventing", instruction = "Never make up profiles", priority = 10))
        val active = repository.getActiveInstructions()
        assertEquals(1, active.size)
        assertEquals("Never make up profiles", active[0].instruction)
    }

    @Test
    fun testTeachCorrectionSystem() = runBlocking {
        repository.recordTeachCorrection(
            customerMessage = "25 saal ki ladki Gorakhpur mein chahiye",
            wrongResponse = "Hamare paas Delhi ke profiles hain.",
            correctResponse = "Ji, Gorakhpur ke 25 saal ke matches check kar ke batate hain."
        )

        val examples = repository.getActiveExamples()
        assertEquals(1, examples.size)
        assertEquals("Ji, Gorakhpur ke 25 saal ke matches check kar ke batate hain.", examples[0].idealResponse)
    }

    @Test
    fun testVersionControlAndRollback() = runBlocking {
        repository.addInstruction(InstructionEntity(title = "Inst 1", instruction = "First instruction"))
        val v1 = repository.createTrainingVersionSnapshot("Version 1", "Initial training")

        repository.addInstruction(InstructionEntity(title = "Inst 2", instruction = "Second instruction"))
        assertEquals(2, repository.getActiveInstructions().size)

        val rolledBack = repository.rollbackToVersion(v1.versionId)
        assertTrue(rolledBack)
        assertEquals(1, repository.getActiveInstructions().size)
        assertEquals("First instruction", repository.getActiveInstructions()[0].instruction)
    }
}
