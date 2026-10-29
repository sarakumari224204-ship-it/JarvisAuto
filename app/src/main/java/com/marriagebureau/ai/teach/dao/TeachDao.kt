package com.marriagebureau.ai.teach.dao

import androidx.room.*
import com.marriagebureau.ai.teach.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeachDao {

    // Identity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveIdentity(identity: AgentIdentityEntity)

    @Query("SELECT * FROM agent_identity WHERE id = 1 LIMIT 1")
    suspend fun getIdentity(): AgentIdentityEntity?

    // Instructions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruction(instruction: InstructionEntity): Long

    @Update
    suspend fun updateInstruction(instruction: InstructionEntity)

    @Query("SELECT * FROM teach_instructions WHERE enabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getActiveInstructions(): List<InstructionEntity>

    @Query("SELECT * FROM teach_instructions ORDER BY priority DESC, id ASC")
    fun getAllInstructionsFlow(): Flow<List<InstructionEntity>>

    @Query("SELECT * FROM teach_instructions ORDER BY priority DESC, id ASC")
    suspend fun getAllInstructions(): List<InstructionEntity>

    @Query("DELETE FROM teach_instructions WHERE id = :id")
    suspend fun deleteInstruction(id: Long)

    @Query("DELETE FROM teach_instructions")
    suspend fun deleteAllInstructions()

    // Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Query("SELECT * FROM teach_rules WHERE enabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getActiveRules(): List<RuleEntity>

    @Query("SELECT * FROM teach_rules ORDER BY priority DESC, id ASC")
    fun getAllRulesFlow(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM teach_rules ORDER BY priority DESC, id ASC")
    suspend fun getAllRules(): List<RuleEntity>

    @Query("DELETE FROM teach_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM teach_rules")
    suspend fun deleteAllRules()

    // Examples
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExample(example: ExampleConversationEntity): Long

    @Query("SELECT * FROM example_conversations WHERE enabled = 1 ORDER BY id DESC")
    suspend fun getActiveExamples(): List<ExampleConversationEntity>

    @Query("SELECT * FROM example_conversations WHERE category = :category AND enabled = 1 ORDER BY id DESC")
    suspend fun getExamplesByCategory(category: ExampleCategory): List<ExampleConversationEntity>

    @Query("SELECT * FROM example_conversations ORDER BY id DESC")
    fun getAllExamplesFlow(): Flow<List<ExampleConversationEntity>>

    @Query("SELECT * FROM example_conversations ORDER BY id DESC")
    suspend fun getAllExamples(): List<ExampleConversationEntity>

    @Query("DELETE FROM example_conversations WHERE id = :id")
    suspend fun deleteExample(id: Long)

    @Query("DELETE FROM example_conversations")
    suspend fun deleteAllExamples()

    // Versions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: TrainingVersionEntity): Long

    @Query("SELECT * FROM training_versions ORDER BY versionId DESC")
    fun getAllVersionsFlow(): Flow<List<TrainingVersionEntity>>

    @Query("SELECT * FROM training_versions WHERE versionId = :versionId LIMIT 1")
    suspend fun getVersionById(versionId: Long): TrainingVersionEntity?

    @Query("UPDATE training_versions SET isActive = 0")
    suspend fun deactivateAllVersions()

    @Query("UPDATE training_versions SET isActive = 1 WHERE versionId = :versionId")
    suspend fun activateVersion(versionId: Long)
}
