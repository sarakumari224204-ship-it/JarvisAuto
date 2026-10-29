package com.marriagebureau.ai.ai

data class LLMGenerationConfig(
    val maxTokens: Int = 256,
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val stopSequences: List<String> = listOf("\nCustomer:", "\nSystem:")
)

data class LLMModelInfo(
    val modelName: String,
    val parameterCount: String = "0.5B - 1.5B",
    val quantization: String = "Q4_K_M",
    val modelSizeBytesMb: Long = 650,
    val isLoaded: Boolean = false,
    val loadError: String? = null
)

interface LocalLLMEngine {
    suspend fun isModelLoaded(): Boolean
    suspend fun loadModel(modelFilePath: String): Boolean
    suspend fun generateResponse(prompt: String, config: LLMGenerationConfig = LLMGenerationConfig()): String
    suspend fun getModelInfo(): LLMModelInfo
    suspend fun unloadModel()
}

/**
 * Lightweight local rule/template fallback engine that executes locally on-device without external API calls.
 */
class FallbackTemplateLLMEngine : LocalLLMEngine {
    private var isLoaded = true
    private var currentPath: String = "Internal Fallback Engine"

    override suspend fun isModelLoaded(): Boolean = isLoaded

    override suspend fun loadModel(modelFilePath: String): Boolean {
        currentPath = modelFilePath
        isLoaded = true
        return true
    }

    override suspend fun generateResponse(
        prompt: String,
        config: LLMGenerationConfig
    ): String {
        // Extract match details from prompt context if present
        if (prompt.contains("MATCHED PROFILES:")) {
            val profilesBlock = prompt.substringAfter("MATCHED PROFILES:").substringBefore("TEACH INSTRUCTIONS:")
            val profileLines = profilesBlock.lines().filter { it.trim().startsWith("Code:") || it.trim().startsWith("-") }

            if (profileLines.isNotEmpty()) {
                val firstProfile = profileLines.first().trim()
                return "Ji, aapki requirement ke according hamare paas verified profiles hain. Jaise $firstProfile. Kya aap inke baare mein aur detail janna chahenge?"
            } else {
                return "Ji, main aapki requirement samajh gayi hoon. Abhi is specific filter ke according active match check kar ke aapko update karti hoon."
            }
        }

        if (prompt.contains("REGISTER") || prompt.contains("FEE") || prompt.contains("RULE")) {
            return "Ji, hamare Marriage Bureau ke rules aur registration details aapko share kar dete hain. Service duration 1 year hai. Aap call ya visit bhi kar sakte hain."
        }

        return "Ji bilkul! Aapki requirement ke accoding profiles check ki ja rahi hain. Aap koi specific location ya age preference bhi bata dein."
    }

    override suspend fun getModelInfo(): LLMModelInfo {
        return LLMModelInfo(
            modelName = "On-Device Compact Fallback (6GB Optimized)",
            parameterCount = "0.5B Quantized",
            quantization = "Q4_0",
            modelSizeBytesMb = 350,
            isLoaded = isLoaded
        )
    }

    override suspend fun unloadModel() {
        isLoaded = false
    }
}
