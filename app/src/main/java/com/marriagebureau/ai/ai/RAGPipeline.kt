package com.marriagebureau.ai.ai

import com.marriagebureau.ai.data.db.entity.CustomerRequirementEntity
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.repository.CustomerRepository
import com.marriagebureau.ai.data.repository.ProfileRepository
import com.marriagebureau.ai.data.repository.ProfileSearchCriteria
import com.marriagebureau.ai.knowledge.repository.KnowledgeRepository
import com.marriagebureau.ai.teach.repository.TeachRepository

data class RAGContextResult(
    val promptContext: String,
    val matchedProfiles: List<MarriageProfileEntity>,
    val customerRequirement: CustomerRequirementEntity?,
    val usedKnowledgeChunks: List<String>
)

class RAGPipeline(
    private val profileRepository: ProfileRepository,
    private val customerRepository: CustomerRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val teachRepository: TeachRepository,
    private val llmEngine: LocalLLMEngine
) {

    suspend fun buildContextAndGenerate(
        customerId: Long,
        incomingMessage: String
    ): Pair<RAGContextResult, String> {

        // 1. Customer Memory
        val requirement = customerRepository.getRequirement(customerId)
        val recentMsgs = customerRepository.getRecentMessages(customerId, limit = 5)

        val targetGender = requirement?.targetGender
        val minAge = requirement?.minAge
        val maxAge = requirement?.maxAge
        val preferredCities = requirement?.preferredCities?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }

        // 2. Profile DB Search
        val searchCriteria = ProfileSearchCriteria(
            gender = targetGender,
            minAge = minAge,
            maxAge = maxAge,
            city = preferredCities?.firstOrNull(),
            freeText = if (targetGender == null && minAge == null) incomingMessage else null
        )

        val foundProfiles = profileRepository.searchProfiles(searchCriteria, limit = 5)

        // Filter out already shown profiles unless customer specifically asks for more
        val shownCodes = requirement?.shownProfileCodes?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
        val unshownProfiles = foundProfiles.filter { !shownCodes.contains(it.profileCode) }
        val finalProfiles = if (unshownProfiles.isNotEmpty()) unshownProfiles else foundProfiles.take(3)

        // 3. Knowledge Base Retrieval
        val kbChunks = knowledgeRepository.searchKnowledge(incomingMessage, limit = 3)
        val kbTextList = kbChunks.map { it.chunkText }

        // 4. Teach AI Rules & Identity
        val identity = teachRepository.getIdentity()
        val instructions = teachRepository.getActiveInstructions()
        val rules = teachRepository.getActiveRules()

        // 5. Build Compact Prompt
        val sb = StringBuilder()
        sb.append("AGENT IDENTITY:\n")
        sb.append("Name: ${identity.agentName}, Bureau: ${identity.bureauName}, Persona: ${identity.tone}\n\n")

        sb.append("TEACH INSTRUCTIONS:\n")
        instructions.take(5).forEach { sb.append("- ${it.instruction}\n") }
        rules.take(5).forEach { sb.append("- Rule: ${it.ruleCondition} -> ${it.actionText}\n") }
        sb.append("\n")

        if (kbTextList.isNotEmpty()) {
            sb.append("KNOWLEDGE BASE FACTS:\n")
            kbTextList.forEach { sb.append("- $it\n") }
            sb.append("\n")
        }

        if (finalProfiles.isNotEmpty()) {
            sb.append("MATCHED PROFILES:\n")
            finalProfiles.forEach { p ->
                sb.append("Code: ${p.profileCode}, Name: ${p.name}, Age: ${p.age}, Gender: ${p.gender}, City: ${p.city}, Edu: ${p.education}, Occ: ${p.occupation}\n")
            }
            sb.append("\n")
        } else {
            sb.append("MATCHED PROFILES: None confirmed in database.\n\n")
        }

        sb.append("RECENT CONVERSATION:\n")
        recentMsgs.forEach { m ->
            sb.append("${m.sender}: ${m.messageText}\n")
        }
        sb.append("CUSTOMER: $incomingMessage\n")
        sb.append("AGENT RESPONSE:")

        val prompt = sb.toString()

        val ragResult = RAGContextResult(
            promptContext = prompt,
            matchedProfiles = finalProfiles,
            customerRequirement = requirement,
            usedKnowledgeChunks = kbTextList
        )

        // Generate response using local LLM
        val rawResponse = llmEngine.generateResponse(prompt)

        return Pair(ragResult, rawResponse)
    }
}
