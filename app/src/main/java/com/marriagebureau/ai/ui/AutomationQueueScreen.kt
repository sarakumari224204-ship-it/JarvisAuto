package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marriagebureau.ai.service.AutomationMode
import com.marriagebureau.ai.service.QueueMessageEntity
import com.marriagebureau.ai.service.QueueStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationQueueScreen(
    queueList: List<QueueMessageEntity> = listOf(
        QueueMessageEntity(messageId = 1L, customerId = 1L, whatsappIdentifier = "+919876543210", messageText = "Need 25 saal ladki profile", status = QueueStatus.SENT, generatedResponse = "Ji, MB101 profile available hai."),
        QueueMessageEntity(messageId = 2L, customerId = 2L, whatsappIdentifier = "+919876543211", messageText = "Photo kab milegi?", status = QueueStatus.READY_TO_SEND, generatedResponse = "Ji, verified photo shared shortly.")
    )
) {
    var selectedMode by remember { mutableStateOf(AutomationMode.DRAFT_ONLY) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("WhatsApp Queue & Automation") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Automation Mode Selection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == AutomationMode.DRAFT_ONLY,
                            onClick = { selectedMode = AutomationMode.DRAFT_ONLY },
                            label = { Text("Draft Only") }
                        )
                        FilterChip(
                            selected = selectedMode == AutomationMode.HUMAN_APPROVAL,
                            onClick = { selectedMode = AutomationMode.HUMAN_APPROVAL },
                            label = { Text("Human Approval") }
                        )
                        FilterChip(
                            selected = selectedMode == AutomationMode.AUTOMATIC_LOW_RISK,
                            onClick = { selectedMode = AutomationMode.AUTOMATIC_LOW_RISK },
                            label = { Text("Low-Risk Auto") }
                        )
                    }
                }
            }

            Text("Persistent Queue Items (${queueList.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(queueList) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.whatsappIdentifier, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(item.status.name, fontSize = 11.sp, color = if (item.status == QueueStatus.SENT) Color(0xFF2E7D32) else Color.DarkGray)
                            }
                            Text("Inbound: ${item.messageText}", fontSize = 12.sp)
                            item.generatedResponse?.let {
                                Text("Generated: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
