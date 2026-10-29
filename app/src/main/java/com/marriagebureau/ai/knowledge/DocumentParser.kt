package com.marriagebureau.ai.knowledge

import java.io.InputStream

data class DocumentParseResult(
    val title: String,
    val fileType: String,
    val extractedText: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class DocumentParser {

    fun parseTextStream(title: String, fileType: String, inputStream: InputStream): DocumentParseResult {
        return try {
            val text = inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) {
                DocumentParseResult(title, fileType, "", false, "Error: File is empty or unreadable.")
            } else {
                DocumentParseResult(title, fileType, text.trim(), true)
            }
        } catch (e: Exception) {
            DocumentParseResult(title, fileType, "", false, "Error reading document stream: ${e.message}")
        }
    }

    fun parsePlainText(title: String, fileType: String, textContent: String): DocumentParseResult {
        val trimmed = textContent.trim()
        return if (trimmed.isBlank()) {
            DocumentParseResult(title, fileType, "", false, "Error: Content is blank.")
        } else {
            DocumentParseResult(title, fileType, trimmed, true)
        }
    }
}
