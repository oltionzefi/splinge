package org.oltionzefi.splinge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.oltionzefi.splinge.model.AlgorithmType
import org.oltionzefi.splinge.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    group: Group,
    onBack: () -> Unit,
    onAlgorithmChange: (AlgorithmType) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Optimization Algorithm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        "Smart algorithm reduces the number of transactions by simplifying debts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        FilterChip(
                            selected = group.algorithmType == AlgorithmType.BASIC,
                            onClick = { onAlgorithmChange(AlgorithmType.BASIC) },
                            label = { Text("Basic") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = group.algorithmType == AlgorithmType.DEBT_SIMPLIFICATION,
                            onClick = { onAlgorithmChange(AlgorithmType.DEBT_SIMPLIFICATION) },
                            label = { Text("Smart") }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        "Selected currency will be used for all reports and balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("€", "$", "£").forEach { curr ->
                            FilterChip(
                                selected = group.currency == curr,
                                onClick = { onCurrencyChange(curr) },
                                label = { Text(curr) }
                            )
                        }
                    }
                }
            }
        }
    }
}
