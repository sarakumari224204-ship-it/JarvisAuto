package com.marriagebureau.ai.ai

import com.marriagebureau.ai.ai.providers.*
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

data class BudgetMetrics(
    val dailyRequestCount: Int = 0,
    val maxDailyRequests: Int = 500,
    val estimatedDailyCostUsd: Double = 0.0,
    val maxDailyBudgetUsd: Double = 2.00,
    val totalMonthlyCostUsd: Double = 0.0
)

class HybridLLMManager(
    val localEngine: LocalLLMEngine,
    private val privacyFilter: PrivacyFilter = PrivacyFilter()
) {

    @Volatile
    var activeProviderType: LLMProviderType = LLMProviderType.FALLBACK_TEMPLATE

    @Volatile
    var privacyMode: PrivacyMode = PrivacyMode.STRICT

    private val providerConfigs = mutableMapOf<LLMProviderType, LLMProviderConfig>()
    private val providers = mutableMapOf<LLMProviderType, LLMProvider>()

    private val dailyRequests = AtomicInteger(0)
    private var dailyCostUsd = 0.0

    init {
        // Register default providers
        val fallbackProvider = FallbackTemplateLLMProvider()
        val localProvider = LocalLLMProvider(localEngine)
        val geminiProvider = GeminiProvider()
        val openAIProvider = OpenAIProvider(LLMProviderType.OPENAI)
        val grokProvider = GrokProvider()
        val nvidiaProvider = NvidiaNimProvider()
        val customProvider = OpenAICompatibleProvider()

        providers[LLMProviderType.FALLBACK_TEMPLATE] = fallbackProvider
        providers[LLMProviderType.LOCAL] = localProvider
        providers[LLMProviderType.GEMINI] = geminiProvider
        providers[LLMProviderType.OPENAI] = openAIProvider
        providers[LLMProviderType.GROK] = grokProvider
        providers[LLMProviderType.NVIDIA_NIM] = nvidiaProvider
        providers[LLMProviderType.OPENAI_COMPATIBLE] = customProvider

        // Set default configs
        LLMProviderType.values().forEach { type ->
            providerConfigs[type] = LLMProviderConfig(providerType = type)
        }
    }

    fun configureProvider(config: LLMProviderConfig) {
        providerConfigs[config.providerType] = config
    }

    fun getProviderConfig(type: LLMProviderType): LLMProviderConfig {
        return providerConfigs[type] ?: LLMProviderConfig(providerType = type)
    }

    suspend fun testConnection(type: LLMProviderType): LLMResponse {
        val provider = providers[type] ?: throw LLMError.ProviderUnavailable("Provider $type not registered.")
        val config = getProviderConfig(type)
        return provider.testConnection(config)
    }

    suspend fun fetchModels(type: LLMProviderType): List<String> {
        val provider = providers[type] ?: return emptyList()
        val config = getProviderConfig(type)
        return try {
            provider.fetchAvailableModels(config)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun generateResponseWithFallback(request: LLMRequest): LLMResponse {
        val currentBudget = getBudgetMetrics()
        if (currentBudget.dailyRequestCount >= currentBudget.maxDailyRequests ||
            currentBudget.estimatedDailyCostUsd >= currentBudget.maxDailyBudgetUsd) {
            // Budget exceeded -> Fall back to offline local execution
            return executeFallbackChain(request, "Daily budget or request limit reached.")
        }

        val primaryType = activeProviderType
        val primaryConfig = getProviderConfig(primaryType)

        if (primaryType == LLMProviderType.FALLBACK_TEMPLATE || primaryType == LLMProviderType.LOCAL) {
            val response = executeProviderWithRetry(primaryType, request, primaryConfig)
            recordUsage(response)
            return response
        }

        // Online execution with Privacy Filtering
        val sanitizedPrompt = privacyFilter.sanitizePrompt(request.prompt, privacyMode)
        val sanitizedRequest = request.copy(prompt = sanitizedPrompt)

        return try {
            val response = executeProviderWithRetry(primaryType, sanitizedRequest, primaryConfig)
            recordUsage(response)
            response
        } catch (e: Exception) {
            if (primaryConfig.enableFallback) {
                executeFallbackChain(request, e.message)
            } else {
                throw e
            }
        }
    }

    private suspend fun executeProviderWithRetry(
        type: LLMProviderType,
        request: LLMRequest,
        config: LLMProviderConfig
    ): LLMResponse {
        val provider = providers[type] ?: throw LLMError.ProviderUnavailable("Provider $type unavailable.")

        var attempts = 0
        var lastError: Exception? = null

        while (attempts < 2) {
            attempts++
            try {
                return provider.generateResponse(request, config)
            } catch (e: LLMError.RateLimited) {
                lastError = e
                delay(1000L * attempts) // Retry backoff
            } catch (e: Exception) {
                lastError = e
                break
            }
        }

        throw lastError ?: LLMError.ProviderUnavailable("Failed execution for $type")
    }

    private suspend fun executeFallbackChain(request: LLMRequest, reason: String?): LLMResponse {
        // Try Local LLM Provider
        val localConfig = getProviderConfig(LLMProviderType.LOCAL)
        try {
            val localResponse = providers[LLMProviderType.LOCAL]?.generateResponse(request, localConfig)
            if (localResponse != null) {
                val finalResp = localResponse.copy(isFromFallback = true)
                recordUsage(finalResp)
                return finalResp
            }
        } catch (e: Exception) {
            // Fallthrough to FallbackTemplate
        }

        // Final Fallback: FallbackTemplateLLMProvider
        val fallbackConfig = getProviderConfig(LLMProviderType.FALLBACK_TEMPLATE)
        val templateResponse = providers[LLMProviderType.FALLBACK_TEMPLATE]!!.generateResponse(request, fallbackConfig)
        val finalResp = templateResponse.copy(isFromFallback = true)
        recordUsage(finalResp)
        return finalResp
    }

    fun getBudgetMetrics(): BudgetMetrics {
        return BudgetMetrics(
            dailyRequestCount = dailyRequests.get(),
            estimatedDailyCostUsd = dailyCostUsd
        )
    }

    private fun recordUsage(response: LLMResponse) {
        dailyRequests.incrementAndGet()
        val metrics = response.metrics ?: return
        val config = getProviderConfig(response.providerType)
        val costPerToken = config.estimatedPricePerMillionTokensUsd / 1_000_000.0
        dailyCostUsd += metrics.totalTokens * costPerToken
    }
}
