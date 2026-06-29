package org.oltionzefi.splinge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.oltionzefi.splinge.logic.SplitCalculator
import org.oltionzefi.splinge.model.AlgorithmType
import org.oltionzefi.splinge.model.Expense
import org.oltionzefi.splinge.model.Group
import org.oltionzefi.splinge.model.Member
import org.oltionzefi.splinge.model.Transaction
import org.oltionzefi.splinge.util.*
import org.oltionzefi.splinge.ui.components.SimpleAlertDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onShare: () -> Unit,
    onShareOverview: () -> Unit,
    onAlgorithmChange: (AlgorithmType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onAddMember: () -> Unit,
    onSettings: () -> Unit,
    onDelete: () -> Unit,
    onShareExpense: (Expense) -> Unit,
    onShareMemberBalance: (Member, Double) -> Unit,
    onShareTransaction: (Transaction) -> Unit,
    onShareExpensesPart: (Int) -> Unit
) {
    val transactions = remember(group.algorithmType, group.expenses, group.members) { SplitCalculator.calculateTransactions(group) }
    val groupedTransactions = remember(transactions) { transactions.groupBy { it.to } }
    val netBalances = remember(group.algorithmType, group.expenses, group.members) { SplitCalculator.calculateNetBalances(group) }
    var showMaxMembersAlert by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }

    if (showShareOptions) {
        Dialog(onDismissRequest = { showShareOptions = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Share Options", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "This group has many expenses. How would you like to share?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            showShareOptions = false
                            onShareOverview()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Overview Only")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (group.expenses.size > 70) {
                        val partsCount = (group.expenses.size + 69) / 70
                        repeat(partsCount) { index ->
                            OutlinedButton(
                                onClick = {
                                    showShareOptions = false
                                    onShareExpensesPart(index)
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Share Expenses part ${index + 1}")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                showShareOptions = false
                                onShare()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share Full List")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showShareOptions = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

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
                    IconButton(onClick = {
                        if (group.expenses.size > 70) {
                            showShareOptions = true
                        } else {
                            onShare()
                        }
                    }) {
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
                                if (net > 0) "is owed ${group.currency}${net.format(2)}"
                                else if (net < 0) "owes ${group.currency}${(-net).format(2)}"
                                else "is settled up",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (net > 0) Color(0xFF4CAF50) else if (net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Balances Section
            if (group.expenses.isNotEmpty()) {
                item {
                    val totalSpent = remember(group.expenses) { SplitCalculator.calculateTotalSpent(group) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Balances", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Total Spent: ${group.currency}${totalSpent.format(2)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (transactions.isEmpty()) {
                    item {
                        Text(
                            "All settled up! 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            if (transactions.isNotEmpty()) {
                groupedTransactions.forEach { (creditorId, txs) ->
                    val creditorName = group.members.find { it.id == creditorId }?.name ?: "Unknown"
                    val totalOwedToCreditor = txs.sumOf { it.amount }

                    item(key = "header_$creditorId") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            creditorName.take(1).uppercase(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$creditorName is owed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${group.currency}${totalOwedToCreditor.format(2)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = {
                                    val member = group.members.find { it.id == creditorId }
                                    if (member != null) {
                                        onShareMemberBalance(member, totalOwedToCreditor)
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Balance", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    items(txs, key = { tx -> "${tx.from}_${tx.to}" }) { tx ->
                        val fromMember = group.members.find { it.id == tx.from }?.name ?: "Unknown"
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 2.dp, bottom = 2.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = fromMember,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${group.currency}${tx.amount.format(2)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                IconButton(onClick = { onShareTransaction(tx) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Payment", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
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
                        onClick = { onEditExpense(expense) },
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
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description, style = MaterialTheme.typography.titleMedium)
                                val splitCount = if (group.algorithmType == AlgorithmType.PERCENTAGE) {
                                    group.members.size
                                } else {
                                    expense.splits.size
                                }
                                Text(
                                    "Paid by $payer • Split among $splitCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${group.currency}${expense.amount.format(2)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            IconButton(onClick = { onShareExpense(expense) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Expense", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
