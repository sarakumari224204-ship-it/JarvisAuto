package com.marriagebureau.ai.ai

import com.marriagebureau.ai.knowledge.DocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class LocalModelAndDocumentTest {

    @Test
    fun testLocalModelManagerFallbackStatus() = runBlocking {
        val fallbackEngine = FallbackTemplateLLMEngine()
        val manager = LocalModelManager(fallbackEngine)

        val status = manager.getStatus()
        assertTrue(status.isConfigured)
        assertTrue(status.statusText.contains("LOADED"))

        val testRes = manager.runTestGeneration("Customer says: 25 saal ki ladki chahiye")
        assertNotNull(testRes)
        assertTrue(testRes.contains("matches") || testRes.contains("requirement"))
    }

    @Test
    fun testDocumentParserStreamAndText() {
        val parser = DocumentParser()

        val sampleText = "Marriage Bureau Rules: Registration fee Rs 1000 for 1 year service."
        val stream = ByteArrayInputStream(sampleText.toByteArray(Charsets.UTF_8))

        val result = parser.parseTextStream("Rules", "TXT", stream)
        assertTrue(result.isSuccess)
        assertEquals("Rules", result.title)
        assertTrue(result.extractedText.contains("Rs 1000"))
    }
}
