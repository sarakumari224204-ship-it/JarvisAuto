package com.marriagebureau.ai.ai

import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import java.util.regex.Pattern

data class ValidationResult(
    val isValid: Boolean,
    val sanitizedResponse: String,
    val isHallucinationBlocked: Boolean,
    val reason: String? = null
)

class ResponseValidator {

    fun validateAndSanitize(
        rawResponse: String,
        matchedProfiles: List<MarriageProfileEntity>
    ): ValidationResult {

        if (rawResponse.isBlank()) {
            return ValidationResult(
                isValid = false,
                sanitizedResponse = "Ji, main aapki requirement samajh gayi hoon. Profile details check karke batati hoon.",
                isHallucinationBlocked = false,
                reason = "Empty raw response"
            )
        }

        val validCodes = matchedProfiles.map { it.profileCode.uppercase() }.toSet()

        // Extract potential profile codes from response text (e.g., MB101, MB-202, CSV101, IMP-101)
        val codePattern = Pattern.compile("([A-Za-z]{2,5}-?\\d{2,6})")
        val matcher = codePattern.matcher(rawResponse)

        val citedCodesInText = mutableSetOf<String>()
        while (matcher.find()) {
            matcher.group(1)?.let {
                citedCodesInText.add(it.uppercase())
            }
        }

        // Hallucination check: if text mentions profile code that does NOT exist in retrieved matched profiles
        val invalidCitedCodes = citedCodesInText.filter { code ->
            !validCodes.contains(code) && !validCodes.contains(code.replace("-", ""))
        }

        if (invalidCitedCodes.isNotEmpty()) {
            return ValidationResult(
                isValid = false,
                sanitizedResponse = "Ji, main aapki requirement ke accoding active verified profiles database mein check kar ke aapko exact details share karti hoon.",
                isHallucinationBlocked = true,
                reason = "Hallucination detected: Cited unconfirmed profile codes $invalidCitedCodes"
            )
        }

        // Check if response claims specific profile details when zero profiles matched
        if (matchedProfiles.isEmpty()) {
            val lower = rawResponse.lowercase()
            if (lower.contains("jaise") || lower.contains("profile code") || lower.contains("saal ki ladki hai")) {
                return ValidationResult(
                    isValid = false,
                    sanitizedResponse = "Ji, abhi aapki exact requirement ke matching confirmed active profiles available nahi hain. Main database update hote hi aapko batati hoon.",
                    isHallucinationBlocked = true,
                    reason = "Hallucination detected: Claimed profile existence when matched list is empty."
                )
            }
        }

        return ValidationResult(
            isValid = true,
            sanitizedResponse = rawResponse.trim(),
            isHallucinationBlocked = false,
            reason = null
        )
    }
}
