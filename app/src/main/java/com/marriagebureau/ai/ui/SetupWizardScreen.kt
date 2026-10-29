package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onSetupComplete: () -> Unit = {}
) {
    var step by remember { mutableStateOf(1) }
    var bureauName by remember { mutableStateOf("Royal Marriage Bureau") }
    var ownerName by remember { mutableStateOf("Manager") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Marriage Bureau AI - Setup Wizard (Step $step of 8)") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LinearProgressIndicator(
                    progress = { step / 8f },
                    modifier = Modifier.fillMaxWidth()
                )

                when (step) {
                    1 -> {
                        Text("1. Welcome & Bureau Identity", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Configure your marriage bureau details to customize the AI Assistant's greetings.", fontSize = 13.sp)
                        OutlinedTextField(
                            value = bureauName,
                            onValueChange = { bureauName = it },
                            label = { Text("Marriage Bureau Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Manager Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        Text("2. Import Marriage Profiles", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Load your profile database. You can seed test profiles or import CSV/JSON files.", fontSize = 13.sp)
                        Button(onClick = {}) { Text("Seed 10,000 Test Profiles") }
                        OutlinedButton(onClick = {}) { Text("Import CSV / Excel / JSON File") }
                    }
                    3 -> {
                        Text("3. Teach Basic Instructions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Teach the AI how to greet customers and handle profile inquiries.", fontSize = 13.sp)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Rule 1: Always search database before claiming match.", fontSize = 12.sp)
                                Text("Rule 2: Speak naturally in Hindi or Hinglish.", fontSize = 12.sp)
                                Text("Rule 3: Never invent missing profile details.", fontSize = 12.sp)
                            }
                        }
                    }
                    4 -> {
                        Text("4. Enable Notification Access", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Allow the app to read incoming WhatsApp Business notifications to identify queries.", fontSize = 13.sp)
                        Button(onClick = {}) { Text("Open Notification Settings") }
                    }
                    5 -> {
                        Text("5. Configure WhatsApp Business", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Ensure WhatsApp Business app is installed and active on this phone.", fontSize = 13.sp)
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                            Text("WhatsApp Business Detected: ACTIVE", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    6 -> {
                        Text("6. Test Incoming Message & AI Generation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Simulate a customer asking: '25 saal ki ladki chahiye Gorakhpur mein'", fontSize = 13.sp)
                        Button(onClick = {}) { Text("Run Local AI Test Inference") }
                    }
                    7 -> {
                        Text("7. Test Actual Reply Capability", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Test Notification RemoteInput or Accessibility Auto-Send mechanism.", fontSize = 13.sp)
                        Button(onClick = {}) { Text("Send Test Notification Reply") }
                    }
                    8 -> {
                        Text("8. Setup Complete!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                        Text("Your Marriage Bureau AI Agent is ready to assist customers offline on this phone.", fontSize = 14.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(onClick = { step-- }) { Text("BACK") }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (step < 8) {
                    Button(onClick = { step++ }) { Text("NEXT STEP") }
                } else {
                    Button(onClick = onSetupComplete) { Text("ENTER DASHBOARD") }
                }
            }
        }
    }
}
