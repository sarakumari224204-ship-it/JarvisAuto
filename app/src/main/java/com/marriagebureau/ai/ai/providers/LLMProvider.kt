package com.marriagebureau.ai.ai.providers

import com.marriagebureau.ai.ai.*
import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    val providerType: LLMProviderType
    suspend fun testConnection(config: LLMProviderConfig): LLMResponse
    suspend fun fetchAvailableModels(config: LLMProviderConfig): List<String>
    suspend fun generateResponse(request: LLMRequest, config: LLMProviderConfig): LLMResponse
    fun generateStreamingResponse(request: LLMRequest, config: LLMProviderConfig): Flow<String>
}
