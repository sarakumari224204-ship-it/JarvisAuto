package com.marriagebureau.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    profilesList: List<MarriageProfileEntity> = listOf(
        MarriageProfileEntity(profileCode = "MB101", name = "Priya Sharma", gender = "FEMALE", age = 25, city = "Gorakhpur", education = "B.Tech", occupation = "Teacher"),
        MarriageProfileEntity(profileCode = "MB102", name = "Rahul Verma", gender = "MALE", age = 28, city = "Lucknow", education = "MBA", occupation = "Bank Manager"),
        MarriageProfileEntity(profileCode = "MB103", name = "Sneha Gupta", gender = "FEMALE", age = 27, city = "Gorakhpur", education = "MBBS", occupation = "Doctor")
    ),
    onSearch: (String) -> Unit = {},
    onAddProfile: () -> Unit = {},
    onImportProfiles: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marriage Profiles (${profilesList.size})") },
                actions = {
                    TextButton(onClick = onImportProfiles) {
                        Text("IMPORT CSV/JSON")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProfile) {
                Icon(Icons.Default.Add, contentDescription = "Add Profile")
            }
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
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search profile code, city, education, name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profilesList) { profile ->
                    ProfileCard(profile)
                }
            }
        }
    }
}

@Composable
fun ProfileCard(profile: MarriageProfileEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${profile.profileCode} - ${profile.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = profile.gender,
                    fontWeight = FontWeight.SemiBold,
                    color = if (profile.gender == "FEMALE") Color(0xFFC2185B) else Color(0xFF1976D2),
                    fontSize = 12.sp
                )
            }

            Text(
                text = "Age: ${profile.age} | City: ${profile.city} | Education: ${profile.education}",
                fontSize = 13.sp
            )
            Text(
                text = "Occupation: ${profile.occupation} | Status: ${profile.status}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
