package com.marriagebureau.ai.ai

import java.io.File

data class ModelConfigStatus(
    val isConfigured: Boolean,
    val modelFileName: String?,
    val modelFilePath: String?,
    val fileSizeBytesMb: Long,
    val estimatedRamMb: Long,
    val isCompatibleWith6GBRam: Boolean,
    val statusText: String
)

class LocalModelManager(
    private val localEngine: LocalLLMEngine
) {

    suspend fun configureModelFile(filePath: String): ModelConfigStatus {
        val file = File(filePath)
        if (!file.exists()) {
            return ModelConfigStatus(
                isConfigured = false,
                modelFileName = null,
                modelFilePath = filePath,
                fileSizeBytesMb = 0,
                estimatedRamMb = 0,
                isCompatibleWith6GBRam = true,
                statusText = "NOT CONFIGURED: File does not exist at $filePath"
            )
        }

        val sizeMb = file.length() / (1024 * 1024)
        val estimatedRam = (sizeMb * 1.3).toLong() + 350 // Quantized model VRAM/RAM overhead estimate

        val loaded = localEngine.loadModel(filePath)
        val isCompatible = estimatedRam < 4500 // Suitable for 6GB Android device

        return ModelConfigStatus(
            isConfigured = loaded,
            modelFileName = file.name,
            modelFilePath = filePath,
            fileSizeBytesMb = sizeMb,
            estimatedRamMb = estimatedRam,
            isCompatibleWith6GBRam = isCompatible,
            statusText = if (loaded) "LOADED: ${file.name} active ($sizeMb MB)" else "ERROR: Failed to initialize model binary"
        )
    }

    suspend fun getStatus(): ModelConfigStatus {
        val info = localEngine.getModelInfo()
        return if (info.isLoaded) {
            ModelConfigStatus(
                isConfigured = true,
                modelFileName = info.modelName,
                modelFilePath = "Local Storage",
                fileSizeBytesMb = info.modelSizeBytesMb,
                estimatedRamMb = (info.modelSizeBytesMb * 1.3).toLong(),
                isCompatibleWith6GBRam = true,
                statusText = "LOADED: ${info.modelName} (${info.quantization})"
            )
        } else {
            ModelConfigStatus(
                isConfigured = false,
                modelFileName = null,
                modelFilePath = null,
                fileSizeBytesMb = 0,
                estimatedRamMb = 0,
                isCompatibleWith6GBRam = true,
                statusText = "NOT CONFIGURED: Using On-Device Compact Fallback Engine"
            )
        }
    }

    suspend fun runTestGeneration(samplePrompt: String): String {
        return localEngine.generateResponse(samplePrompt)
    }
}
