package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marriagebureau.ai.service.EmergencyStopController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profileCount: Int = 10000,
    customerCount: Int = 42,
    pendingMessages: Int = 0,
    sentMessages: Int = 18,
    failedMessages: Int = 0,
    lastIncoming: String = "25 saal ki ladki chahiye Gorakhpur mein",
    lastResponse: String = "Ji, Gorakhpur ke 25 saal ke matches available hain. Details share ki ja rahi hain.",
    onEmergencyStop: () -> Unit = {}
) {
    var isEmergencyStopped by remember { mutableStateOf(EmergencyStopController.isEmergencyStopped) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marriage Bureau AI - Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emergency STOP Banner & Button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEmergencyStopped) Color(0xFFFFCDD2) else Color(0xFFE8F5E9)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEmergencyStopped) "EMERGENCY STOP ACTIVE" else "AUTOMATION ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isEmergencyStopped) Color(0xFFB71C1C) else Color(0xFF2E7D32)
                        )
                        Text(
                            text = if (isEmergencyStopped) "All background queue & WhatsApp responses halted." else "System ready. Local LLM & Queue running.",
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmergencyStopped) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        ),
                        onClick = {
                            if (isEmergencyStopped) {
                                EmergencyStopController.resetEmergencyStop()
                                isEmergencyStopped = false
                            } else {
                                EmergencyStopController.triggerEmergencyStop()
                                isEmergencyStopped = true
                                onEmergencyStop()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEmergencyStopped) "RESUME" else "EMERGENCY STOP")
                    }
                }
            }

            // Quick Stats Grid
            Text("Overview Metrics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard("Marriage Profiles", "$profileCount", Modifier.weight(1f))
                MetricCard("Customers", "$customerCount", Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard("Pending Queue", "$pendingMessages", Modifier.weight(1f))
                MetricCard("Sent Replies", "$sentMessages", Modifier.weight(1f))
                MetricCard("Failed Replies", "$failedMessages", Modifier.weight(1f))
            }

            // Status Cards
            Text("System Runtime Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRow("Local LLM Engine", "0.5B Quantized Fallback (Active)")
                    StatusRow("Automation Mode", "Draft Only / Human Approval")
                    StatusRow("WhatsApp Access", "NotificationListener Service Bound")
                    StatusRow("Database Memory", "10,000 Profiles Indexed")
                }
            }

            // Live Activity
            Text("Recent Conversation Activity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Last Customer Message:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(lastIncoming, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)

                    HorizontalDivider()

                    Text("Last AI Agent Response:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(lastResponse, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20))
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    }
}
