package com.marriagebureau.ai.ai.providers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.marriagebureau.ai.ai.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

open class OpenAIProvider(
    override val providerType: LLMProviderType = LLMProviderType.OPENAI,
    private val gson: Gson = Gson()
) : LLMProvider {

    override suspend fun testConnection(config: LLMProviderConfig): LLMResponse {
        val testRequest = LLMRequest(prompt = "Hello", maxTokens = 10)
        return generateResponse(testRequest, config)
    }

    override suspend fun fetchAvailableModels(config: LLMProviderConfig): List<String> {
        return when (providerType) {
            LLMProviderType.OPENAI -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
            LLMProviderType.GROK -> listOf("grok-beta", "grok-2-vision-1212")
            LLMProviderType.NVIDIA_NIM -> listOf("meta/llama-3.1-8b-instruct", "meta/llama-3.1-70b-instruct")
            else -> listOf("default-model")
        }
    }

    override suspend fun generateResponse(request: LLMRequest, config: LLMProviderConfig): LLMResponse {
        val apiKey = config.apiKey.ifBlank { throw LLMError.InvalidApiKey("API key is blank for $providerType.") }

        val defaultBaseUrl = when (providerType) {
            LLMProviderType.OPENAI -> "https://api.openai.com/v1"
            LLMProviderType.GROK -> "https://api.x.ai/v1"
            LLMProviderType.NVIDIA_NIM -> "https://integrate.api.nvidia.com/v1"
            else -> "https://api.openai.com/v1"
        }

        val baseUrl = config.baseUrl.ifBlank { defaultBaseUrl }.removeSuffix("/")
        val model = config.modelName.ifBlank {
            when (providerType) {
                LLMProviderType.OPENAI -> "gpt-4o-mini"
                LLMProviderType.GROK -> "grok-beta"
                LLMProviderType.NVIDIA_NIM -> "meta/llama-3.1-8b-instruct"
                else -> "gpt-3.5-turbo"
            }
        }

        val urlString = "$baseUrl/chat/completions"

        val messagesArray = JsonArray()
        request.systemPrompt?.let { sys ->
            val sysObj = JsonObject()
            sysObj.addProperty("role", "system")
            sysObj.addProperty("content", sys)
            messagesArray.add(sysObj as JsonElement)
        }

        val userObj = JsonObject()
        userObj.addProperty("role", "user")
        userObj.addProperty("content", request.prompt)
        messagesArray.add(userObj as JsonElement)

        val requestBodyObj = JsonObject()
        requestBodyObj.addProperty("model", model)
        requestBodyObj.addProperty("temperature", request.temperature ?: config.temperature)
        requestBodyObj.addProperty("max_tokens", request.maxTokens ?: config.maxTokens)
        requestBodyObj.add("messages", messagesArray)

        var responseText = ""
        var inTokens = 0
        var outTokens = 0

        val latency = measureTimeMillis {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                config.headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

                connection.connectTimeout = config.timeoutSeconds * 1000
                connection.readTimeout = config.timeoutSeconds * 1000
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use { it.write(requestBodyObj.toString()) }

                val code = connection.responseCode
                if (code in 200..299) {
                    val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = gson.fromJson(rawJson, JsonObject::class.java)

                    val choices = jsonObj.getAsJsonArray("choices")
                    if (choices != null && choices.size() > 0) {
                        val firstChoice = choices.get(0).asJsonObject
                        val msg = firstChoice.getAsJsonObject("message")
                        responseText = msg?.get("content")?.asString ?: ""
                    }

                    jsonObj.getAsJsonObject("usage")?.let { usage ->
                        inTokens = usage.get("prompt_tokens")?.asInt ?: 0
                        outTokens = usage.get("completion_tokens")?.asInt ?: 0
                    }
                } else {
                    val errStream = connection.errorStream
                    val errText = if (errStream != null) errStream.bufferedReader().use { it.readText() } else ""
                    handleHttpError(code, errText)
                }
            } catch (e: LLMError) {
                throw e
            } catch (e: Exception) {
                throw LLMError.NetworkError("$providerType Connection Error: ${e.message}")
            } finally {
                connection.disconnect()
            }
        }

        val totalTokens = inTokens + outTokens
        val metrics = LLMTokenMetrics(
            inputTokens = inTokens,
            outputTokens = outTokens,
            totalTokens = totalTokens,
            generationTimeMs = latency,
            tokensPerSecond = if (latency > 0 && outTokens > 0) (outTokens * 1000f) / latency else 0f
        )

        return LLMResponse(
            content = responseText.trim(),
            providerType = providerType,
            modelName = model,
            latencyMs = latency,
            metrics = metrics
        )
    }

    override fun generateStreamingResponse(request: LLMRequest, config: LLMProviderConfig): Flow<String> = flow {
        val res = generateResponse(request, config)
        val chunks = res.content.split("\\s+".toRegex())
        for (chunk in chunks) {
            emit("$chunk ")
            delay(25)
        }
    }

    private fun handleHttpError(code: Int, errorText: String) {
        when (code) {
            401 -> throw LLMError.Unauthorized("401 Unauthorized API Key.")
            403 -> throw LLMError.Forbidden("403 Forbidden Access.")
            404 -> throw LLMError.ModelNotFound("404 Model Not Found.")
            429 -> throw LLMError.RateLimited("429 Rate Limit Exceeded.")
            in 500..599 -> throw LLMError.ServerError("500 Server Error ($code).")
            else -> throw LLMError.BadRequest("HTTP $code: $errorText")
        }
    }
}
