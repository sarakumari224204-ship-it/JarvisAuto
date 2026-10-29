package com.marriagebureau.ai.ui

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Profiles : Screen("profiles", "Marriage Profiles")
    object Customers : Screen("customers", "Customers & Chat")
    object TeachAI : Screen("teach_ai", "Teach AI")
    object Knowledge : Screen("knowledge", "Knowledge Base")
    object Automation : Screen("automation", "WhatsApp Queue")
    object Settings : Screen("settings", "Settings")
    object Diagnostics : Screen("diagnostics", "System Diagnostics")
}
