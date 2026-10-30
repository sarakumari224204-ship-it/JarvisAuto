package com.marriagebureau.ai.ai

import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HybridLLMAndPrivacyTest {

    private lateinit var privacyFilter: PrivacyFilter
    private lateinit var hybridLLMManager: HybridLLMManager

    @Before
    fun setUp() {
        privacyFilter = PrivacyFilter()
        val localEngine = FallbackTemplateLLMEngine()
        hybridLLMManager = HybridLLMManager(localEngine, privacyFilter)
    }

    @Test
    fun testPrivacyFilterStripping() {
        val rawProfile = MarriageProfileEntity(
            profileId = 101L,
            profileCode = "MB101",
            name = "Priya Sharma",
            gender = "FEMALE",
            age = 25,
            city = "Gorakhpur",
            contactInformation = "9988776655",
            familyInformation = "Private Family Details"
        )

        val strictList = privacyFilter.filterProfilesForPrivacy(listOf(rawProfile), PrivacyMode.STRICT)
        assertEquals(1, strictList.size)
        assertNull(strictList[0].contactInformation)
        assertNull(strictList[0].familyInformation)

        val sanitizedPrompt = privacyFilter.sanitizePrompt("Contact me at +919876543210 or email user@example.com", PrivacyMode.STRICT)
        assertTrue(sanitizedPrompt.contains("[CONTACT_REDACTED]"))
        assertTrue(sanitizedPrompt.contains("[EMAIL_REDACTED]"))
    }

    @Test
    fun testHybridManagerFallbackToLocal() = runBlocking {
        // Configure active provider as Gemini without key (will fail)
        hybridLLMManager.activeProviderType = LLMProviderType.GEMINI
        hybridLLMManager.configureProvider(
            LLMProviderConfig(providerType = LLMProviderType.GEMINI, apiKey = "", enableFallback = true)
        )

        val request = LLMRequest(prompt = "25 saal ki ladki MATCHED PROFILES:\nCode: MB101\nTEACH INSTRUCTIONS:")
        val response = hybridLLMManager.generateResponseWithFallback(request)

        assertNotNull(response)
        assertTrue(response.isFromFallback)
        assertTrue(response.content.isNotBlank())
    }

    @Test
    fun testBudgetMetricsTracking() = runBlocking {
        val request = LLMRequest(prompt = "Hello")
        hybridLLMManager.generateResponseWithFallback(request)

        val budget = hybridLLMManager.getBudgetMetrics()
        assertTrue(budget.dailyRequestCount > 0)
    }
}
