package com.marriagebureau.ai.ai.providers

import com.marriagebureau.ai.ai.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

class FallbackTemplateLLMProvider(
    private val fallbackEngine: FallbackTemplateLLMEngine = FallbackTemplateLLMEngine()
) : LLMProvider {

    override val providerType: LLMProviderType = LLMProviderType.FALLBACK_TEMPLATE

    override suspend fun testConnection(config: LLMProviderConfig): LLMResponse {
        return generateResponse(LLMRequest(prompt = "Hello"), config)
    }

    override suspend fun fetchAvailableModels(config: LLMProviderConfig): List<String> {
        return listOf("On-Device Compact Fallback")
    }

    override suspend fun generateResponse(request: LLMRequest, config: LLMProviderConfig): LLMResponse {
        var text = ""
        val latency = measureTimeMillis {
            text = fallbackEngine.generateResponse(request.prompt)
        }

        val outTokens = text.split("\\s+".toRegex()).size
        val metrics = LLMTokenMetrics(
            inputTokens = request.prompt.split("\\s+".toRegex()).size,
            outputTokens = outTokens,
            totalTokens = request.prompt.split("\\s+".toRegex()).size + outTokens,
            generationTimeMs = latency,
            tokensPerSecond = if (latency > 0) (outTokens * 1000f) / latency else 0f
        )

        return LLMResponse(
            content = text,
            providerType = providerType,
            modelName = "On-Device Compact Fallback Engine",
            latencyMs = latency,
            metrics = metrics,
            isFromFallback = true
        )
    }

    override fun generateStreamingResponse(request: LLMRequest, config: LLMProviderConfig): Flow<String> = flow {
        val res = generateResponse(request, config)
        val chunks = res.content.split("\\s+".toRegex())
        for (chunk in chunks) {
            emit("$chunk ")
            delay(15)
        }
    }
}
