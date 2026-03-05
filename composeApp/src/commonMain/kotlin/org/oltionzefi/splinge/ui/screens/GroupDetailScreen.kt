package org.oltionzefi.splinge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.oltionzefi.splinge.logic.SplitCalculator
import org.oltionzefi.splinge.model.AlgorithmType
import org.oltionzefi.splinge.model.Group
import org.oltionzefi.splinge.ui.components.SimpleAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onShare: () -> Unit,
    onAlgorithmChange: (AlgorithmType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onAddMember: () -> Unit,
    onSettings: () -> Unit,
    onDelete: () -> Unit
) {
    val transactions = remember(group.algorithmType, group.expenses, group.members) { SplitCalculator.calculateTransactions(group) }
    val netBalances = remember(group.expenses, group.members) { SplitCalculator.calculateNetBalances(group) }
    var showMaxMembersAlert by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showMaxMembersAlert) {
        SimpleAlertDialog(
            onDismissRequest = { showMaxMembersAlert = false },
            title = "Limit reached",
            text = "You have reached the maximum of 10 members in this group."
        )
    }

    if (showDeleteConfirmation) {
        SimpleAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = "Delete Group",
            text = "Are you sure you want to delete this group? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            }
        )
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(group.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        if (showMenu) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, 50),
                                onDismissRequest = { showMenu = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.width(200.dp)
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showMenu = false
                                                    onSettings()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Text("Group Settings", style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showMenu = false
                                                    showDeleteConfirmation = true
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    "Delete Group",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpense,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expense") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // Members Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Members", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        if (group.members.size < 10) {
                            onAddMember()
                        } else {
                            showMaxMembersAlert = true
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            items(group.members) { member ->
                val net = netBalances[member.id] ?: 0.0
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(member.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (net > 0) "is owed ${group.currency}$net"
                                else if (net < 0) "owes ${group.currency}${-net}"
                                else "is settled up",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (net > 0) Color(0xFF4CAF50) else if (net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Balances Section
            if (transactions.isNotEmpty()) {
                item {
                    Text("Balances", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))
                }
                items(transactions) { tx ->
                    val fromMember = group.members.find { it.id == tx.from }?.name ?: "Unknown"
                    val toMember = group.members.find { it.id == tx.to }?.name ?: "Unknown"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = buildString {
                                        append(fromMember)
                                        append(" owes ")
                                        append(toMember)
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${group.currency}${tx.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Expenses Section
            item {
                Text("Expenses", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))
            }
            if (group.expenses.isEmpty()) {
                item {
                    Text(
                        "No expenses yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(group.expenses.reversed()) { expense ->
                    val payer = group.members.find { it.id == expense.paidById }?.name ?: "Unknown"
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Paid by $payer • Split among ${expense.splits.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${group.currency}${expense.amount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
