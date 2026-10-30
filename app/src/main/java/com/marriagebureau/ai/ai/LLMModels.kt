package com.marriagebureau.ai.ai

enum class LLMProviderType(val displayName: String) {
    LOCAL("Offline Local Model"),
    GEMINI("Google Gemini API"),
    OPENAI("OpenAI / ChatGPT API"),
    GROK("xAI Grok API"),
    NVIDIA_NIM("NVIDIA NIM API"),
    OPENAI_COMPATIBLE("Custom OpenAI-Compatible API"),
    FALLBACK_TEMPLATE("On-Device Fallback Engine")
}

enum class PrivacyMode {
    STRICT,      // Default: Minimum profile fields only, no contact/private metadata
    STANDARD,    // Matched profiles + relevant knowledge chunks
    OFFLINE_ONLY // Never transmit data outside device
}

data class LLMTokenMetrics(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = 0,
    val timeToFirstTokenMs: Long = 0,
    val generationTimeMs: Long = 0,
    val tokensPerSecond: Float = 0f
)

data class LLMProviderConfig(
    val providerType: LLMProviderType = LLMProviderType.FALLBACK_TEMPLATE,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val temperature: Float = 0.3f,
    val maxTokens: Int = 256,
    val timeoutSeconds: Int = 15,
    val enableStreaming: Boolean = false,
    val enableFallback: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val estimatedPricePerMillionTokensUsd: Double = 0.50
)

data class LLMRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList()
)

data class LLMResponse(
    val content: String,
    val providerType: LLMProviderType,
    val modelName: String,
    val latencyMs: Long,
    val metrics: LLMTokenMetrics? = null,
    val isFromFallback: Boolean = false
)

sealed class LLMError(val userFriendlyMessage: String) : Exception(userFriendlyMessage) {
    class InvalidApiKey(msg: String = "Invalid API key configured.") : LLMError(msg)
    class Unauthorized(msg: String = "401 Unauthorized request.") : LLMError(msg)
    class Forbidden(msg: String = "403 Access Forbidden.") : LLMError(msg)
    class RateLimited(msg: String = "429 Rate limit exceeded. Retrying...") : LLMError(msg)
    class ModelNotFound(msg: String = "Requested model not found.") : LLMError(msg)
    class BadRequest(msg: String = "Bad request configuration.") : LLMError(msg)
    class ServerError(msg: String = "Provider server error.") : LLMError(msg)
    class NetworkError(msg: String = "Network connection failed.") : LLMError(msg)
    class Timeout(msg: String = "Request timed out.") : LLMError(msg)
    class ContentBlocked(msg: String = "Response blocked by safety filter.") : LLMError(msg)
    class DailyBudgetExceeded(msg: String = "Daily AI request budget limit reached.") : LLMError(msg)
    class ProviderUnavailable(msg: String = "AI provider currently unavailable.") : LLMError(msg)
}
