package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var responseStyle by remember { mutableStateOf("Hinglish - Friendly & Professional") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Application Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AI Inference Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = "Compact On-Device Fallback (0.5B parameters)",
                onValueChange = {},
                readOnly = true,
                label = { Text("Loaded LLM Model") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Response Language & Tone", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = responseStyle,
                onValueChange = { responseStyle = it },
                label = { Text("Default Response Style") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Database Backup & Privacy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {}) { Text("Export Encrypted Backup") }
                OutlinedButton(onClick = {}) { Text("Clear Customer Memory") }
            }
        }
    }
}
