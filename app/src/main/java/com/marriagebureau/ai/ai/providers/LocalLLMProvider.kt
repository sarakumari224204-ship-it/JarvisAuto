package com.marriagebureau.ai.ai.providers

import com.marriagebureau.ai.ai.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

class LocalLLMProvider(
    private val localEngine: LocalLLMEngine
) : LLMProvider {

    override val providerType: LLMProviderType = LLMProviderType.LOCAL

    override suspend fun testConnection(config: LLMProviderConfig): LLMResponse {
        val request = LLMRequest(prompt = "Hello")
        return generateResponse(request, config)
    }

    override suspend fun fetchAvailableModels(config: LLMProviderConfig): List<String> {
        val info = localEngine.getModelInfo()
        return listOf(info.modelName)
    }

    override suspend fun generateResponse(request: LLMRequest, config: LLMProviderConfig): LLMResponse {
        var text = ""
        val latency = measureTimeMillis {
            text = localEngine.generateResponse(request.prompt)
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
            modelName = localEngine.getModelInfo().modelName,
            latencyMs = latency,
            metrics = metrics
        )
    }

    override fun generateStreamingResponse(request: LLMRequest, config: LLMProviderConfig): Flow<String> = flow {
        val res = generateResponse(request, config)
        val chunks = res.content.split("\\s+".toRegex())
        for (chunk in chunks) {
            emit("$chunk ")
            delay(20)
        }
    }
}
