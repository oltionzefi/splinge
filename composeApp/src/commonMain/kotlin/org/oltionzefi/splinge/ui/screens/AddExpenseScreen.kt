package org.oltionzefi.splinge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.oltionzefi.splinge.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    group: Group,
    onBack: () -> Unit,
    onSave: (String, Double, String, List<String>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidById by remember { mutableStateOf(group.members.firstOrNull()?.id ?: "") }
    val paidForIds = remember { mutableStateListOf<String>().apply { addAll(group.members.map { it.id }) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What was it for?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                )
            }
            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text(group.currency, modifier = Modifier.padding(start = 12.dp)) }
                )
            }
            item {
                Text("Paid by", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.members.forEach { member ->
                        FilterChip(
                            selected = paidById == member.id,
                            onClick = { paidById = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }
            }
            item {
                Text("Split among", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(group.members) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = paidForIds.contains(member.id),
                        onCheckedChange = { checked ->
                            if (checked) paidForIds.add(member.id)
                            else if (paidForIds.size > 1) paidForIds.remove(member.id)
                        }
                    )
                    Text(member.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (description.isNotBlank() && amount > 0 && paidForIds.isNotEmpty()) {
                            onSave(description, amount, paidById, paidForIds.toList())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && paidForIds.isNotEmpty()
                ) {
                    Text("Save Expense", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
