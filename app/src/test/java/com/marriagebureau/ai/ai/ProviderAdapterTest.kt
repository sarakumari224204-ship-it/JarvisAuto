package com.marriagebureau.ai.ai

import com.marriagebureau.ai.ai.providers.*
import com.marriagebureau.ai.security.SecureStorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProviderAdapterTest {

    @Test
    fun testSecureStorageManagerMasking() {
        val secret = "sk-1234567890abcdef"
        val masked = SecureStorageManager.maskSecret(secret)
        assertEquals("sk-1...cdef", masked)

        val nullMasked = SecureStorageManager.maskSecret(null)
        assertEquals("Not Configured", nullMasked)
    }

    @Test
    fun testFallbackTemplateLLMProvider() = runBlocking {
        val provider = FallbackTemplateLLMProvider()
        val config = LLMProviderConfig(providerType = LLMProviderType.FALLBACK_TEMPLATE)
        val request = LLMRequest(prompt = "25 saal ki ladki chahiye Gorakhpur mein MATCHED PROFILES:\nCode: MB101, Name: Priya, Age: 25, City: Gorakhpur\nTEACH INSTRUCTIONS:")

        val response = provider.generateResponse(request, config)
        assertNotNull(response)
        assertTrue(response.isFromFallback)
        assertTrue(response.content.contains("MB101") || response.content.contains("verified profiles"))
    }

    @Test
    fun testLocalLLMProviderAdapter() = runBlocking {
        val localEngine = FallbackTemplateLLMEngine()
        val provider = LocalLLMProvider(localEngine)
        val config = LLMProviderConfig(providerType = LLMProviderType.LOCAL)

        val response = provider.generateResponse(LLMRequest(prompt = "Test local prompt"), config)
        assertNotNull(response)
        assertEquals(LLMProviderType.LOCAL, response.providerType)
    }
}
