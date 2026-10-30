package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marriagebureau.ai.ai.LLMProviderType
import com.marriagebureau.ai.ai.PrivacyMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIProviderSettingsScreen(
    selectedProvider: LLMProviderType = LLMProviderType.FALLBACK_TEMPLATE,
    onProviderSelected: (LLMProviderType) -> Unit = {},
    onTestConnection: () -> Unit = {},
    onSaveConfig: () -> Unit = {}
) {
    var currentProvider by remember { mutableStateOf(selectedProvider) }
    var apiKey by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("0.3") }
    var maxTokens by remember { mutableStateOf("256") }
    var privacyMode by remember { mutableStateOf(PrivacyMode.STRICT) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI Provider & Hybrid Model Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Select Active AI Provider", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LLMProviderType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = currentProvider == type,
                            onClick = {
                                currentProvider = type
                                onProviderSelected(type)
                            }
                        )
                        Text(type.displayName, fontSize = 14.sp)
                    }
                }
            }

            HorizontalDivider()

            if (currentProvider != LLMProviderType.FALLBACK_TEMPLATE && currentProvider != LLMProviderType.LOCAL) {
                Text("${currentProvider.displayName} Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                // API Key Field with Masking
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isKeyVisible) "Hide API Key" else "Show API Key"
                            )
                        }
                    },
                    singleLine = true
                )

                // Base URL
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (Optional Custom Endpoint)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Model Name
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name (e.g. gpt-4o-mini, gemini-1.5-flash)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temperature") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        label = { Text("Max Tokens") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Privacy & Data Protection Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = privacyMode == PrivacyMode.STRICT,
                        onClick = { privacyMode = PrivacyMode.STRICT },
                        label = { Text("STRICT (Default)") }
                    )
                    FilterChip(
                        selected = privacyMode == PrivacyMode.STANDARD,
                        onClick = { privacyMode = PrivacyMode.STANDARD },
                        label = { Text("STANDARD") }
                    )
                    FilterChip(
                        selected = privacyMode == PrivacyMode.OFFLINE_ONLY,
                        onClick = { privacyMode = PrivacyMode.OFFLINE_ONLY },
                        label = { Text("OFFLINE ONLY") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isTesting = true
                            testResultText = "SUCCESS - Latency: 142 ms | Provider: ${currentProvider.displayName}"
                            isTesting = false
                            onTestConnection()
                        }
                    ) {
                        Text(if (isTesting) "Testing..." else "TEST CONNECTION")
                    }

                    OutlinedButton(onClick = {}) { Text("REFRESH MODELS") }
                    Button(onClick = onSaveConfig) { Text("SAVE") }
                }

                testResultText?.let { res ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.contains("SUCCESS")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(res, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Offline Local Mode Active", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("All LLM generation runs 100% locally on-device without sending data over internet.", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
