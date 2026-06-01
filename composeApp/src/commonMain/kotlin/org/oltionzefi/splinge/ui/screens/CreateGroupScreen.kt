package org.oltionzefi.splinge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.oltionzefi.splinge.model.UserSettings
import org.oltionzefi.splinge.ui.components.NameInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    userSettings: UserSettings,
    onBack: () -> Unit,
    onSave: (String, Boolean, String?) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var addMe by remember { mutableStateOf(userSettings.name.isNotBlank()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("New Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    singleLine = true
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (userSettings.name.isBlank()) 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Add myself to this group",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (userSettings.name.isBlank() && newUserName == null) {
                                Text(
                                    "Tap to set your name and add yourself",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    "As member: ${newUserName ?: userSettings.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = addMe,
                            onCheckedChange = { 
                                addMe = it 
                                if (addMe && userSettings.name.isBlank() && newUserName == null) {
                                    showNameDialog = true
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (groupName.isNotBlank()) onSave(groupName, addMe, newUserName) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = groupName.isNotBlank() && (!addMe || userSettings.name.isNotBlank() || newUserName != null)
                ) {
                    Text("Create Group")
                }
            }
        }

        if (showNameDialog) {
            NameInputDialog(
                onDismissRequest = { 
                    showNameDialog = false
                    if (newUserName == null) addMe = false
                },
                onSave = { name ->
                    newUserName = name
                    showNameDialog = false
                }
            )
        }
    }
}
