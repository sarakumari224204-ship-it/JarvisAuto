package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marriagebureau.ai.data.db.entity.ConversationMessageEntity
import com.marriagebureau.ai.data.db.entity.CustomerEntity
import com.marriagebureau.ai.data.db.entity.HandoffStatus
import com.marriagebureau.ai.data.db.entity.MessageSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    customers: List<CustomerEntity> = listOf(
        CustomerEntity(customerId = 1L, whatsappIdentifier = "+919876543210", displayName = "Ramesh Kumar", handoffStatus = HandoffStatus.AUTOMATED),
        CustomerEntity(customerId = 2L, whatsappIdentifier = "+919876543211", displayName = "Suresh Gupta", handoffStatus = HandoffStatus.HUMAN_REQUIRED)
    ),
    sampleMessages: List<ConversationMessageEntity> = listOf(
        ConversationMessageEntity(customerId = 1L, sender = MessageSender.CUSTOMER, messageText = "25 saal ki ladki chahiye Gorakhpur mein"),
        ConversationMessageEntity(customerId = 1L, sender = MessageSender.AGENT, messageText = "Ji, MB101 profile available hai. Details check karke batate hain.")
    )
) {
    var selectedCustomerId by remember { mutableStateOf<Long?>(1L) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Customers & Conversation Memory") })
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column: Customer list
            Card(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Customer Directory", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customers) { customer ->
                            Card(
                                onClick = { selectedCustomerId = customer.customerId },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCustomerId == customer.customerId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(customer.displayName.ifBlank { customer.whatsappIdentifier }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        text = if (customer.handoffStatus == HandoffStatus.HUMAN_REQUIRED) "HUMAN REQUIRED" else "AUTOMATED",
                                        fontSize = 11.sp,
                                        color = if (customer.handoffStatus == HandoffStatus.HUMAN_REQUIRED) Color.Red else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right column: Chat & Requirement details
            Card(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Extracted Memory & Requirement", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Target Gender: FEMALE", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Age Range: 23 - 27 years", fontSize = 12.sp)
                            Text("Location: Gorakhpur", fontSize = 12.sp)
                            Text("Education: B.Tech / Graduate", fontSize = 12.sp)
                            Text("Shown Profiles: MB101", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Text("Conversation History", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sampleMessages) { msg ->
                            val isCustomer = msg.sender == MessageSender.CUSTOMER
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isCustomer) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCustomer) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE8F5E9)
                                    ),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = if (isCustomer) "Customer" else "AI Agent",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (isCustomer) Color.Gray else Color(0xFF2E7D32)
                                        )
                                        Text(msg.messageText, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
