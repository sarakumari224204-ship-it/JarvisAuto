package com.marriagebureau.ai.ai

import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity

class PrivacyFilter {

    fun filterProfilesForPrivacy(
        profiles: List<MarriageProfileEntity>,
        mode: PrivacyMode
    ): List<MarriageProfileEntity> {
        return when (mode) {
            PrivacyMode.OFFLINE_ONLY -> emptyList() // Do not transmit profiles externally in OFFLINE_ONLY mode
            PrivacyMode.STRICT -> profiles.map { p ->
                p.copy(
                    profileId = 0L,
                    contactInformation = null, // Strip private contact details
                    familyInformation = null,  // Strip private family metadata
                    photoReferences = ""       // Strip photo URIs
                )
            }
            PrivacyMode.STANDARD -> profiles.map { p ->
                p.copy(
                    profileId = 0L,
                    contactInformation = null // Strip contact details
                )
            }
        }
    }

    fun sanitizePrompt(prompt: String, mode: PrivacyMode): String {
        if (mode == PrivacyMode.OFFLINE_ONLY) {
            return "OFFLINE_ONLY_RESTRICTION"
        }
        // Remove phone numbers or email patterns from outgoing online prompts
        val phonePattern = "(\\+?\\d{10,12}|\\d{3}[-\\s]?\\d{3}[-\\s]?\\d{4})".toRegex()
        val emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()

        return prompt
            .replace(phonePattern, "[CONTACT_REDACTED]")
            .replace(emailPattern, "[EMAIL_REDACTED]")
    }
}
