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
import com.marriagebureau.ai.teach.entity.InstructionEntity
import com.marriagebureau.ai.teach.entity.RuleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachAIScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Identity", "Instructions", "Rules", "Examples", "Test Agent", "Versions")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Teach AI System") })
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> IdentitySubTab()
                1 -> InstructionsSubTab()
                2 -> RulesSubTab()
                3 -> ExamplesSubTab()
                4 -> TestAgentSubTab()
                5 -> VersionsSubTab()
            }
        }
    }
}

@Composable
fun IdentitySubTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Agent Identity Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        OutlinedTextField(value = "Sara", onValueChange = {}, label = { Text("Agent Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = "Royal Marriage Bureau", onValueChange = {}, label = { Text("Bureau Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = "Senior Marriage Assistant", onValueChange = {}, label = { Text("Agent Role") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = "Hindi, Hinglish, English", onValueChange = {}, label = { Text("Supported Languages") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {}) { Text("Save Identity") }
    }
}

@Composable
fun InstructionsSubTab() {
    val instructions = listOf(
        InstructionEntity(title = "No Inventing", instruction = "Never make up a marriage profile.", priority = 10),
        InstructionEntity(title = "Natural Language", instruction = "Speak naturally in Hindi/Hinglish unless customer uses English.", priority = 8)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Owner Custom Instructions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Button(onClick = {}) { Text("Add Instruction") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(instructions) { inst ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(inst.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(inst.instruction, fontSize = 12.sp)
                        Text("Priority: ${inst.priority} | Enabled: ${inst.enabled}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun RulesSubTab() {
    val rules = listOf(
        RuleEntity(title = "Age Search Rule", ruleCondition = "Customer asks for specific age range", actionText = "Query database first, state confirmed active matches only", priority = 10, isSystemSafetyRule = true)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Business & Safety Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rules) { rule ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(rule.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("IF: ${rule.ruleCondition}", fontSize = 12.sp)
                        Text("THEN: ${rule.actionText}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ExamplesSubTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Example Conversations", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CUSTOMER: 25 saal ki ladki chahiye Gorakhpur mein.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text("IDEAL RESPONSE: Ji bilkul! Main Gorakhpur ke 25 saal ke matches check karti hoon.", fontSize = 12.sp, color = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun TestAgentSubTab() {
    var testInput by remember { mutableStateOf("Customer says: 27 saal ki ladki chahiye Gorakhpur mein") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Test Agent & Reasoning Inspector", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        OutlinedTextField(
            value = testInput,
            onValueChange = { testInput = it },
            label = { Text("Test Message Input") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {}) { Text("Run Test Inference") }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Inspection Results:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("1. Detected Intent: PROFILE_SEARCH", fontSize = 12.sp)
                Text("2. Requirements: Gender=FEMALE, Age=27, City=Gorakhpur", fontSize = 12.sp)
                Text("3. Matched Profiles Searched: 1 found (MB103)", fontSize = 12.sp)
                Text("4. Generated Response: Ji, Gorakhpur ke matches check kar ke aapko batate hain.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("5. Reason: Follows instruction #1 & matched profile MB103", fontSize = 11.sp, color = Color.DarkGray)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Accept") }
                    OutlinedButton(onClick = {}) { Text("Teach Correction") }
                }
            }
        }
    }
}

@Composable
fun VersionsSubTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Training Version Control History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Training Version 1 (Active)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Initial setup with 2 custom instructions & 1 safety rule.", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Create New Snapshot") }
                    OutlinedButton(onClick = {}) { Text("Rollback") }
                }
            }
        }
    }
}
