package com.marriagebureau.ai.data.importexport

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.db.entity.ProfileStatus
import com.marriagebureau.ai.data.repository.ProfileRepository

data class ImportRecordValidation(
    val rawContent: String,
    val profile: MarriageProfileEntity?,
    val isValid: Boolean,
    val isDuplicate: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

data class ImportPreviewResult(
    val totalRecordsFound: Int,
    val validCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val validations: List<ImportRecordValidation>
)

data class ImportExecutionResult(
    val importedCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val failedCount: Int,
    val importedIds: List<Long>
)

class ProfileImporter(
    private val profileRepository: ProfileRepository,
    private val gson: Gson = Gson()
) {

    suspend fun parseAndPreviewCsv(csvContent: String): ImportPreviewResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportPreviewResult(0, 0, 0, 0, emptyList())
        }

        val header = lines.first().split(",").map { it.trim().lowercase() }
        val dataLines = lines.drop(1)

        val validations = mutableListOf<ImportRecordValidation>()
        var validCount = 0
        var duplicateCount = 0
        var invalidCount = 0

        dataLines.forEachIndexed { index, line ->
            val tokens = parseCsvLine(line)
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            fun getVal(key: String): String? {
                val idx = header.indexOf(key)
                return if (idx >= 0 && idx < tokens.size) tokens[idx].trim() else null
            }

            val code = getVal("code") ?: getVal("profilecode") ?: "IMP-${System.currentTimeMillis()}-${index + 1}"
            val name = getVal("name") ?: getVal("fullname") ?: ""
            val gender = (getVal("gender") ?: "MALE").uppercase()
            val ageStr = getVal("age") ?: "0"
            val age = ageStr.toIntOrNull() ?: 0
            val city = getVal("city") ?: ""
            val education = getVal("education") ?: ""
            val occupation = getVal("occupation") ?: ""
            val contact = getVal("contact") ?: getVal("phone") ?: ""

            if (name.isBlank()) errors.add("Missing required field 'name'")
            if (city.isBlank()) warnings.add("Missing field 'city'")
            if (age <= 0 || age > 100) errors.add("Invalid age: $ageStr")
            if (gender != "MALE" && gender != "FEMALE") errors.add("Invalid gender: $gender")

            val profile = if (errors.isEmpty()) {
                MarriageProfileEntity(
                    profileCode = code,
                    name = name,
                    gender = gender,
                    age = age,
                    city = city,
                    education = education,
                    occupation = occupation,
                    contactInformation = contact.ifBlank { null },
                    source = "CSV"
                )
            } else null

            val isDuplicate = if (profile != null) {
                profileRepository.findDuplicates(profile).isNotEmpty()
            } else false

            if (isDuplicate) {
                duplicateCount++
                warnings.add("Duplicate record detected")
            }

            val isValid = errors.isEmpty()
            if (isValid) validCount++ else invalidCount++

            validations.add(
                ImportRecordValidation(
                    rawContent = line,
                    profile = profile,
                    isValid = isValid,
                    isDuplicate = isDuplicate,
                    errors = errors,
                    warnings = warnings
                )
            )
        }

        return ImportPreviewResult(
            totalRecordsFound = dataLines.size,
            validCount = validCount,
            duplicateCount = duplicateCount,
            invalidCount = invalidCount,
            validations = validations
        )
    }

    suspend fun parseAndPreviewJson(jsonContent: String): ImportPreviewResult {
        val validations = mutableListOf<ImportRecordValidation>()
        var validCount = 0
        var duplicateCount = 0
        var invalidCount = 0

        try {
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val rawList: List<Map<String, Any?>> = gson.fromJson(jsonContent, type) ?: emptyList()

            rawList.forEachIndexed { index, map ->
                val errors = mutableListOf<String>()
                val warnings = mutableListOf<String>()

                val code = map["profileCode"]?.toString() ?: map["code"]?.toString() ?: "JSON-${index + 1}"
                val name = map["name"]?.toString() ?: ""
                val gender = (map["gender"]?.toString() ?: "MALE").uppercase()
                val age = (map["age"] as? Double)?.toInt() ?: map["age"]?.toString()?.toIntOrNull() ?: 0
                val city = map["city"]?.toString() ?: ""
                val education = map["education"]?.toString() ?: ""
                val occupation = map["occupation"]?.toString() ?: ""
                val contact = map["contactInformation"]?.toString() ?: map["contact"]?.toString()

                if (name.isBlank()) errors.add("Missing required field 'name'")
                if (city.isBlank()) warnings.add("Missing field 'city'")
                if (age <= 0 || age > 100) errors.add("Invalid age: $age")

                val profile = if (errors.isEmpty()) {
                    MarriageProfileEntity(
                        profileCode = code,
                        name = name,
                        gender = gender,
                        age = age,
                        city = city,
                        education = education,
                        occupation = occupation,
                        contactInformation = contact,
                        source = "JSON"
                    )
                } else null

                val isDuplicate = if (profile != null) {
                    profileRepository.findDuplicates(profile).isNotEmpty()
                } else false

                if (isDuplicate) duplicateCount++
                val isValid = errors.isEmpty()
                if (isValid) validCount++ else invalidCount++

                validations.add(
                    ImportRecordValidation(
                        rawContent = map.toString(),
                        profile = profile,
                        isValid = isValid,
                        isDuplicate = isDuplicate,
                        errors = errors,
                        warnings = warnings
                    )
                )
            }
            return ImportPreviewResult(rawList.size, validCount, duplicateCount, invalidCount, validations)
        } catch (e: Exception) {
            return ImportPreviewResult(0, 0, 0, 0, emptyList())
        }
    }

    suspend fun executeImport(
        preview: ImportPreviewResult,
        skipDuplicates: Boolean = true
    ): ImportExecutionResult {
        val toImport = preview.validations
            .filter { it.isValid && (!skipDuplicates || !it.isDuplicate) }
            .mapNotNull { it.profile }

        if (toImport.isEmpty()) {
            return ImportExecutionResult(0, preview.duplicateCount, preview.invalidCount, 0, emptyList())
        }

        return try {
            val insertedIds = profileRepository.addProfilesBulk(toImport)
            ImportExecutionResult(
                importedCount = insertedIds.size,
                duplicateCount = preview.duplicateCount,
                invalidCount = preview.invalidCount,
                failedCount = 0,
                importedIds = insertedIds
            )
        } catch (e: Exception) {
            ImportExecutionResult(
                importedCount = 0,
                duplicateCount = preview.duplicateCount,
                invalidCount = preview.invalidCount,
                failedCount = toImport.size,
                importedIds = emptyList()
            )
        }
    }

    suspend fun rollbackImport(importedIds: List<Long>): Boolean {
        return try {
            importedIds.forEach { id ->
                profileRepository.deleteProfile(id)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString())
        return result
    }
}
