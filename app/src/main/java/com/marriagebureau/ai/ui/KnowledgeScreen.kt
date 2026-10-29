package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marriagebureau.ai.knowledge.entity.KnowledgeDocumentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    documents: List<KnowledgeDocumentEntity> = listOf(
        KnowledgeDocumentEntity(documentId = 1L, title = "Bureau Rules & Service Fees", fileType = "TXT", totalChunks = 2),
        KnowledgeDocumentEntity(documentId = 2L, title = "Official FAQ Web Page", fileType = "URL", sourceUrlOrPath = "https://example.com/faq", totalChunks = 1)
    )
) {
    var searchKeyword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Knowledge Base") },
                actions = {
                    TextButton(onClick = {}) { Text("IMPORT DOC") }
                    TextButton(onClick = {}) { Text("FETCH URL") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                label = { Text("Search local knowledge chunks...") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Indexed Knowledge Documents", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(documents) { doc ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Type: ${doc.fileType} | Chunks Indexed: ${doc.totalChunks}", fontSize = 12.sp)
                            if (doc.sourceUrlOrPath.isNotBlank()) {
                                Text("Source: ${doc.sourceUrlOrPath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
