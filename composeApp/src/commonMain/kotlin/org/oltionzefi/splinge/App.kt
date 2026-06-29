package org.oltionzefi.splinge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.oltionzefi.splinge.db.AppRepository
import org.oltionzefi.splinge.logic.SplitCalculator
import org.oltionzefi.splinge.model.*
import org.oltionzefi.splinge.navigation.Screen
import org.oltionzefi.splinge.ui.components.PaypalInputDialog
import org.oltionzefi.splinge.ui.components.SimpleAlertDialog
import org.oltionzefi.splinge.ui.components.BackHandler
import org.oltionzefi.splinge.ui.screens.*
import org.oltionzefi.splinge.ui.theme.SplingeTheme
import org.oltionzefi.splinge.util.*

@Composable
fun App() {
    val platform = getPlatform()
    val repository = remember { AppRepository(getDatabaseDriverFactory(), platform.ioDispatcher) }
    val scope = rememberCoroutineScope()

    SplingeTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.GroupList) }

        BackHandler(enabled = currentScreen != Screen.GroupList) {
            currentScreen = when (val screen = currentScreen) {
                is Screen.GroupDetail -> Screen.GroupList
                is Screen.AddExpense -> Screen.GroupDetail(screen.groupId)
                is Screen.EditExpense -> Screen.GroupDetail(screen.groupId)
                is Screen.AddMember -> Screen.GroupDetail(screen.groupId)
                is Screen.GroupSettings -> Screen.GroupDetail(screen.groupId)
                is Screen.CreateGroup -> Screen.GroupList
                is Screen.GlobalSettings -> Screen.GroupList
                Screen.GroupList -> Screen.GroupList
            }
        }

        // Collect the live group list from the database
        val groupsFlow = remember { repository.groups }
        val groupsState by groupsFlow.collectAsState(initial = null)
        val groups = groupsState ?: emptyList()

        // Settings are loaded once; mutations are persisted immediately
        var userSettings by remember {
            mutableStateOf<UserSettings?>(null)
        }

        // Load settings asynchronously
        LaunchedEffect(Unit) {
            val settings = try {
                repository.loadSettings()
            } catch (e: Exception) {
                UserSettings()
            }
            userSettings = settings
        }

        val settings = userSettings ?: UserSettings()

        var showMaxGroupsAlert by remember { mutableStateOf(false) }
        var showPaypalInputDialog by remember { mutableStateOf(false) }

        if (showMaxGroupsAlert) {
            SimpleAlertDialog(
                onDismissRequest = { showMaxGroupsAlert = false },
                title = "Limit reached",
                text = "You have reached the maximum of 10 groups. Please delete previous groups before adding a new one."
            )
        }

        if (showPaypalInputDialog && userSettings != null) {
            val currentPaypalSettings = userSettings!!
            PaypalInputDialog(
                initialValue = currentPaypalSettings.paypalMe,
                onDismissRequest = { showPaypalInputDialog = false },
                onSave = { newLink ->
                    val updated = currentPaypalSettings.copy(paypalMe = newLink)
                    userSettings = updated
                    showPaypalInputDialog = false
                    scope.launch { repository.saveSettings(updated) }
                    platform.shareText("Pay me on PayPal: ${ShareUtil.generateProfilePaypalLink(newLink)}", "Splinge: My PayPal")
                }
            )
        }

        // Seed sample data only when the DB is empty on first ever launch
        LaunchedEffect(userSettings, groupsState) {
            val currentSettings = userSettings ?: return@LaunchedEffect
            val currentGroups = groupsState ?: return@LaunchedEffect

            if (!currentSettings.isSeeded) {
                if (currentGroups.isEmpty()) {
                    val members = listOf(
                        Member("1", "Alice", "alice"),
                        Member("2", "Bob", "bob"),
                        Member("3", "Charlie")
                    )
                    val expenseAmount = 60.0
                    val splitAmount = roundToTwoDecimals(expenseAmount / members.size)
                    repository.saveGroup(
                        Group(
                            id = "1",
                            name = "Trip to Paris",
                            members = members,
                            expenses = listOf(
                                Expense(
                                    id = "1",
                                    description = "Dinner",
                                    amount = expenseAmount,
                                    paidById = "1",
                                    splits = members.map { Split(it.id, splitAmount) }
                                )
                            )
                        )
                    )
                }
                val updatedSettings = currentSettings.copy(isSeeded = true)
                userSettings = updatedSettings
                repository.saveSettings(updatedSettings)
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.GroupList -> {
                    GroupListScreen(
                        groups = groups,
                        onGroupClick = { currentScreen = Screen.GroupDetail(it.id) },
                        onAddGroup = {
                            if (groups.size >= 10) showMaxGroupsAlert = true
                            else currentScreen = Screen.CreateGroup
                        },
                        onSettings = { currentScreen = Screen.GlobalSettings },
                        onOpenPayme = {
                            val currentListSettings = userSettings
                            if (currentListSettings != null && currentListSettings.paypalMe.isNotBlank()) {
                                platform.shareText("Pay me on PayPal: ${ShareUtil.generateProfilePaypalLink(currentListSettings.paypalMe)}", "Splinge: My PayPal")
                            } else {
                                showPaypalInputDialog = true
                            }
                        }
                    )
                }

                is Screen.GlobalSettings -> {
                    val currentGlobalSettings = userSettings ?: UserSettings()
                    GlobalSettingsScreen(
                        settings = currentGlobalSettings,
                        onBack = { currentScreen = Screen.GroupList },
                        onSave = { updated ->
                            userSettings = updated
                            scope.launch { repository.saveSettings(updated) }
                        }
                    )
                }

                is Screen.CreateGroup -> {
                    val currentCreateSettings = userSettings ?: UserSettings()
                    CreateGroupScreen(
                        userSettings = currentCreateSettings,
                        onBack = { currentScreen = Screen.GroupList },
                        onSave = { groupName, addMe, newUserName ->
                            val finalUserName = newUserName ?: currentCreateSettings.name
                            
                            // If a new name was entered during group creation, save it to settings
                            if (newUserName != null && newUserName.isNotBlank()) {
                                val updatedSettings = currentCreateSettings.copy(name = newUserName)
                                userSettings = updatedSettings
                                scope.launch { repository.saveSettings(updatedSettings) }
                            }

                            val members = mutableListOf<Member>()
                            if (addMe && finalUserName.isNotBlank()) {
                                members.add(Member("me", finalUserName, currentCreateSettings.paypalMe))
                            }
                            val newGroupId = if (groups.isEmpty()) "1" else {
                                val maxId = groups.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0
                                (maxId + 1).toString()
                            }
                            val newGroup = Group(
                                id = newGroupId,
                                name = groupName,
                                members = members
                            )
                            scope.launch { repository.saveGroup(newGroup) }
                            currentScreen = Screen.GroupList
                        }
                    )
                }

                is Screen.GroupDetail -> {
                    val group = groups.find { it.id == screen.groupId }
                    if (group != null) {
                        GroupDetailScreen(
                            group = group,
                            onBack = { currentScreen = Screen.GroupList },
                            onAddExpense = { currentScreen = Screen.AddExpense(group.id) },
                            onEditExpense = { expense -> currentScreen = Screen.EditExpense(group.id, expense.id) },
                            onShare = {
                                scope.launch {
                                    val totalCount = repository.getExpenseCount(group.id)
                                    val allExpenses = mutableListOf<Expense>()
                                    var offset = 0L
                                    val dbBatchSize = 50L
                                    
                                    while (offset < totalCount) {
                                        val batch = repository.getExpensesPaginated(group.id, dbBatchSize, offset)
                                        if (batch.isEmpty()) break
                                        allExpenses.addAll(batch)
                                        offset += dbBatchSize
                                    }
                                    
                                    val groupWithAllExpenses = group.copy(expenses = allExpenses)
                                    val transactions = SplitCalculator.calculateTransactions(groupWithAllExpenses)

                                    val report = ShareUtil.generateReport(groupWithAllExpenses, transactions)
                                    
                                    // Fallback splitting by character count if still too large
                                    val maxMessageSize = 15000
                                    if (report.length > maxMessageSize) {
                                        val parts = report.chunked(maxMessageSize)
                                        parts.forEachIndexed { idx, part ->
                                            platform.shareText(part, "Splinge Report Part ${idx + 1}: ${group.name}")
                                        }
                                    } else {
                                        platform.shareText(report, "Splinge Report: ${group.name}")
                                    }
                                }
                            },
                            onShareOverview = {
                                scope.launch {
                                    val totalCount = repository.getExpenseCount(group.id)
                                    val allExpenses = mutableListOf<Expense>()
                                    var offset = 0L
                                    val batchSize = 50L
                                    
                                    while (offset < totalCount) {
                                        val batch = repository.getExpensesPaginated(group.id, batchSize, offset)
                                        if (batch.isEmpty()) break
                                        allExpenses.addAll(batch)
                                        offset += batchSize
                                    }
                                    
                                    val groupWithAllExpenses = group.copy(expenses = allExpenses)
                                    val transactions = SplitCalculator.calculateTransactions(groupWithAllExpenses)
                                    val report = ShareUtil.generateReport(groupWithAllExpenses, transactions, includeExpenses = false)
                                    platform.shareText(report, "Splinge Overview: ${group.name}")
                                }
                            },
                            onAlgorithmChange = { newType ->
                                scope.launch { repository.saveGroup(group.copy(algorithmType = newType)) }
                            },
                            onCurrencyChange = { newCurrency ->
                                scope.launch { repository.saveGroup(group.copy(currency = newCurrency)) }
                            },
                            onAddMember = { currentScreen = Screen.AddMember(group.id) },
                            onSettings = { currentScreen = Screen.GroupSettings(group.id) },
                            onDelete = {
                                scope.launch { repository.deleteGroup(group.id) }
                                currentScreen = Screen.GroupList
                            },
                            onShareExpense = { expense ->
                                val report = ShareUtil.generateExpenseReport(group, expense)
                                platform.shareText(report, "Expense: ${expense.description}")
                            },
                            onShareMemberBalance = { member, balance ->
                                val report = ShareUtil.generateMemberBalanceReport(group, member, balance)
                                platform.shareText(report, "Balance for ${member.name}")
                            },
                            onShareTransaction = { transaction ->
                                val report = ShareUtil.generateTransactionReport(group, transaction)
                                platform.shareText(report, "Payment Request")
                            },
                            onShareExpensesPart = { index ->
                                scope.launch {
                                    val totalCount = repository.getExpenseCount(group.id)
                                    val allExpenses = mutableListOf<Expense>()
                                    var offset = 0L
                                    val dbBatchSize = 50L
                                    
                                    while (offset < totalCount) {
                                        val batch = repository.getExpensesPaginated(group.id, dbBatchSize, offset)
                                        if (batch.isEmpty()) break
                                        allExpenses.addAll(batch)
                                        offset += dbBatchSize
                                    }
                                    
                                    val groupWithAllExpenses = group.copy(expenses = allExpenses)
                                    val chunkSize = 70
                                    val startIdx = index * chunkSize
                                    val endIdx = (startIdx + chunkSize - 1).coerceAtMost(allExpenses.size - 1)
                                    
                                    if (startIdx < allExpenses.size) {
                                        val partReport = ShareUtil.generateReport(
                                            group = groupWithAllExpenses,
                                            transactions = emptyList(),
                                            includeExpenses = true,
                                            expenseRange = startIdx..endIdx,
                                            includeOverview = false
                                        )
                                        platform.shareText(partReport, "Expenses part ${index + 1}: ${group.name}")
                                    }
                                }
                            }
                        )
                    } else {
                        currentScreen = Screen.GroupList
                    }
                }

                is Screen.GroupSettings -> {
                    val group = groups.find { it.id == screen.groupId }
                    if (group != null) {
                        GroupSettingsScreen(
                            group = group,
                            onBack = { currentScreen = Screen.GroupDetail(group.id) },
                            onAlgorithmChange = { newType ->
                                scope.launch { repository.saveGroup(group.copy(algorithmType = newType)) }
                            },
                            onCurrencyChange = { newCurrency ->
                                scope.launch { repository.saveGroup(group.copy(currency = newCurrency)) }
                            },
                            onMembersUpdate = { updatedMembers ->
                                scope.launch { repository.saveGroup(group.copy(members = updatedMembers)) }
                            }
                        )
                    } else {
                        currentScreen = Screen.GroupList
                    }
                }

                is Screen.AddMember -> {
                    val group = groups.find { it.id == screen.groupId }
                    if (group != null) {
                        AddMemberScreen(
                            onBack = { currentScreen = Screen.GroupDetail(group.id) },
                            onSave = { name ->
                                if (group.members.size < 10) {
                                    val maxId = group.members.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0
                                    val newMember = Member(
                                        id = (maxId + 1).toString(),
                                        name = name
                                    )
                                    scope.launch {
                                        repository.saveGroup(group.copy(members = group.members + newMember))
                                    }
                                    currentScreen = Screen.GroupDetail(group.id)
                                }
                            }
                        )
                    } else {
                        currentScreen = Screen.GroupList
                    }
                }

                is Screen.EditExpense -> {
                    val group = groups.find { it.id == screen.groupId }
                    val expense = group?.expenses?.find { it.id == screen.expenseId }
                    if (group != null && expense != null) {
                        AddExpenseScreen(
                            group = group,
                            expense = expense,
                            onBack = { currentScreen = Screen.GroupDetail(group.id) },
                            onDelete = {
                                val updatedExpenses = group.expenses.filter { it.id != expense.id }
                                scope.launch {
                                    repository.saveGroup(group.copy(expenses = updatedExpenses))
                                }
                                currentScreen = Screen.GroupDetail(group.id)
                            },
                            onSave = { description, amount, paidBy, paidFor ->
                                val roundedAmount = roundToTwoDecimals(amount)
                                val splitAmount = if (paidFor.isNotEmpty()) {
                                    roundToTwoDecimals(roundedAmount / paidFor.size)
                                } else 0.0
                                
                                // Adjust the last split to avoid rounding errors
                                val splits = paidFor.mapIndexed { index, memberId ->
                                    if (index == paidFor.size - 1) {
                                        val totalOtherSplits = splitAmount * (paidFor.size - 1)
                                        Split(memberId, roundToTwoDecimals(roundedAmount - totalOtherSplits))
                                    } else {
                                        Split(memberId, splitAmount)
                                    }
                                }

                                val updatedExpense = expense.copy(
                                    description = description,
                                    amount = roundedAmount,
                                    paidById = paidBy,
                                    splits = splits
                                )
                                val updatedExpenses = group.expenses.map {
                                    if (it.id == expense.id) updatedExpense else it
                                }
                                scope.launch {
                                    repository.saveGroup(group.copy(expenses = updatedExpenses))
                                }
                                currentScreen = Screen.GroupDetail(group.id)
                            }
                        )
                    } else {
                        currentScreen = Screen.GroupList
                    }
                }

                is Screen.AddExpense -> {
                    val group = groups.find { it.id == screen.groupId }
                    if (group != null) {
                        AddExpenseScreen(
                            group = group,
                            onBack = { currentScreen = Screen.GroupDetail(group.id) },
                            onSave = { description, amount, paidBy, paidFor ->
                                val roundedAmount = roundToTwoDecimals(amount)
                                val splitAmount = if (paidFor.isNotEmpty()) {
                                    roundToTwoDecimals(roundedAmount / paidFor.size)
                                } else 0.0

                                // Adjust the last split to avoid rounding errors
                                val splits = paidFor.mapIndexed { index, memberId ->
                                    if (index == paidFor.size - 1) {
                                        val totalOtherSplits = splitAmount * (paidFor.size - 1)
                                        Split(memberId, roundToTwoDecimals(roundedAmount - totalOtherSplits))
                                    } else {
                                        Split(memberId, splitAmount)
                                    }
                                }

                                val maxId = group.expenses.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0
                                val newExpense = Expense(
                                    id = (maxId + 1).toString(),
                                    description = description,
                                    amount = roundedAmount,
                                    paidById = paidBy,
                                    splits = splits
                                )
                                scope.launch {
                                    repository.saveGroup(group.copy(expenses = group.expenses + newExpense))
                                }
                                currentScreen = Screen.GroupDetail(group.id)
                            }
                        )
                    } else {
                        currentScreen = Screen.GroupList
                    }
                }
            }
        }
    }
}
