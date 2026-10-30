package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marriagebureau.ai.ai.LLMProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarriageBureauAppUI() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    var activeProviderType by remember { mutableStateOf(LLMProviderType.FALLBACK_TEMPLATE) }

    val navigationItems = listOf(
        Screen.Dashboard to Icons.Default.Dashboard,
        Screen.Profiles to Icons.Default.People,
        Screen.Customers to Icons.AutoMirrored.Filled.Chat,
        Screen.TeachAI to Icons.Default.School,
        Screen.Knowledge to Icons.AutoMirrored.Filled.MenuBook,
        Screen.AIProviderSettings to Icons.Default.SmartToy,
        Screen.Automation to Icons.Default.AutoMode,
        Screen.Settings to Icons.Default.Settings,
        Screen.Diagnostics to Icons.Default.Build
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marriage Bureau AI") },
                actions = {
                    AssistChip(
                        onClick = { navController.navigate(Screen.AIProviderSettings.route) },
                        label = { Text("AI: ${activeProviderType.name}") }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navigationItems.take(5).forEach { (screen, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Profiles.route) { ProfilesScreen() }
            composable(Screen.Customers.route) { CustomersScreen() }
            composable(Screen.TeachAI.route) { TeachAIScreen() }
            composable(Screen.Knowledge.route) { KnowledgeScreen() }
            composable(Screen.AIProviderSettings.route) {
                AIProviderSettingsScreen(
                    selectedProvider = activeProviderType,
                    onProviderSelected = { activeProviderType = it }
                )
            }
            composable(Screen.Automation.route) { AutomationQueueScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Diagnostics.route) { DiagnosticsScreen() }
            composable(Screen.SetupWizard.route) {
                SetupWizardScreen(onSetupComplete = { navController.navigate(Screen.Dashboard.route) })
            }
        }
    }
}
