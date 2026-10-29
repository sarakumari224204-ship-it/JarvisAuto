package com.marriagebureau.ai.service

enum class AutomationMode {
    DRAFT_ONLY,       // Default: Generates draft/copy action
    HUMAN_APPROVAL,   // Requires human explicit approve before sending
    AUTOMATIC_LOW_RISK // Sends automatically for low risk verified queries
}

object EmergencyStopController {
    @Volatile
    var isEmergencyStopped: Boolean = false
        private set

    fun triggerEmergencyStop(): Boolean {
        isEmergencyStopped = true
        return true
    }

    fun resetEmergencyStop(): Boolean {
        isEmergencyStopped = false
        return true
    }
}
