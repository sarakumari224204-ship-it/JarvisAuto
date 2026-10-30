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

class GeminiProvider(private val gson: Gson = Gson()) : LLMProvider {

    override val providerType: LLMProviderType = LLMProviderType.GEMINI

    override suspend fun testConnection(config: LLMProviderConfig): LLMResponse {
        val testRequest = LLMRequest(prompt = "Hello", maxTokens = 10)
        return generateResponse(testRequest, config)
    }

    override suspend fun fetchAvailableModels(config: LLMProviderConfig): List<String> {
        return listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro")
    }

    override suspend fun generateResponse(request: LLMRequest, config: LLMProviderConfig): LLMResponse {
        val apiKey = config.apiKey.ifBlank { throw LLMError.InvalidApiKey("Gemini API key is blank.") }
        val model = config.modelName.ifBlank { "gemini-1.5-flash" }
        val baseUrl = config.baseUrl.ifBlank { "https://generativelanguage.googleapis.com/v1beta/models" }

        val urlString = "$baseUrl/$model:generateContent?key=$apiKey"

        val partObj = JsonObject()
        partObj.addProperty("text", request.prompt)

        val partsArray = JsonArray()
        partsArray.add(partObj as JsonElement)

        val contentObj = JsonObject()
        contentObj.add("parts", partsArray)

        val contentsArray = JsonArray()
        contentsArray.add(contentObj as JsonElement)

        val genConfigObj = JsonObject()
        genConfigObj.addProperty("temperature", request.temperature ?: config.temperature)
        genConfigObj.addProperty("maxOutputTokens", request.maxTokens ?: config.maxTokens)

        val requestBodyObj = JsonObject()
        requestBodyObj.add("contents", contentsArray)
        requestBodyObj.add("generationConfig", genConfigObj)

        var responseText = ""
        var inTokens = 0
        var outTokens = 0

        val latency = measureTimeMillis {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = config.timeoutSeconds * 1000
                connection.readTimeout = config.timeoutSeconds * 1000
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use { it.write(requestBodyObj.toString()) }

                val code = connection.responseCode
                if (code in 200..299) {
                    val rawJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = gson.fromJson(rawJson, JsonObject::class.java)

                    val candidates = jsonObj.getAsJsonArray("candidates")
                    if (candidates != null && candidates.size() > 0) {
                        val first = candidates.get(0).asJsonObject
                        val content = first.getAsJsonObject("content")
                        val parts = content.getAsJsonArray("parts")
                        if (parts != null && parts.size() > 0) {
                            responseText = parts.get(0).asJsonObject.get("text")?.asString ?: ""
                        }
                    }

                    jsonObj.getAsJsonObject("usageMetadata")?.let { usage ->
                        inTokens = usage.get("promptTokenCount")?.asInt ?: 0
                        outTokens = usage.get("candidatesTokenCount")?.asInt ?: 0
                    }
                } else {
                    val errStream = connection.errorStream
                    val errText = if (errStream != null) errStream.bufferedReader().use { it.readText() } else ""
                    handleHttpError(code, errText)
                }
            } catch (e: LLMError) {
                throw e
            } catch (e: Exception) {
                throw LLMError.NetworkError("Gemini connection error: ${e.message}")
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
            delay(30)
        }
    }

    private fun handleHttpError(code: Int, errorText: String) {
        when (code) {
            401 -> throw LLMError.Unauthorized("Gemini API key is unauthorized.")
            403 -> throw LLMError.Forbidden("Gemini access forbidden.")
            404 -> throw LLMError.ModelNotFound("Gemini model not found.")
            429 -> throw LLMError.RateLimited("Gemini quota rate limited.")
            in 500..599 -> throw LLMError.ServerError("Gemini server error ($code).")
            else -> throw LLMError.BadRequest("Gemini HTTP Error $code: $errorText")
        }
    }
}
