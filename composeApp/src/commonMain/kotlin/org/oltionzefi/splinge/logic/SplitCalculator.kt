package org.oltionzefi.splinge.logic

import org.oltionzefi.splinge.model.*
import org.oltionzefi.splinge.util.*
import kotlin.math.abs
import kotlin.math.round

object SplitCalculator {

    /**
     * Calculates the minimum set of transactions required to settle all debts within a group.
     * Supports two algorithms:
     * - BASIC: Aggregates all expenses and simplifies bidirectional debts.
     * - DEBT_SIMPLIFICATION: Uses a greedy algorithm to minimize the total number of transactions.
     * - PERCENTAGE: Splitting logic is ignored per expense and instead calculated based on group-level member percentages.
     */
    fun calculateTransactions(group: Group): List<Transaction> {
        return when (group.algorithmType) {
            AlgorithmType.BASIC -> calculateBasic(group)
            AlgorithmType.DEBT_SIMPLIFICATION, AlgorithmType.PERCENTAGE -> {
                val netBalances = calculateNetBalances(group)
                calculateSimplified(netBalances)
            }
        }
    }

    /**
     * Calculates the net balance for each member in the group.
     * A positive balance means the member is owed money, while a negative balance means they owe money.
     *
     * In PERCENTAGE mode, it calculates each member's share of every expense based on their
     * pre-defined percentage in the group, regardless of the individual expense splits.
     */
    fun calculateNetBalances(group: Group): Map<String, Double> {
        val netBalances = mutableMapOf<String, Double>()
        
        // Initialize balances
        group.members.forEach { netBalances[it.id] = 0.0 }

        if (group.algorithmType == AlgorithmType.PERCENTAGE) {
            val totalPct = group.members.sumOf { it.percentage }
            if (abs(totalPct - 100.0) > 0.01) return netBalances
        }
        
        // Calculate net balance for each member
        for (expense in group.expenses) {
            // Payer is credited the full amount
            netBalances[expense.paidById] = roundToTwoDecimals((netBalances[expense.paidById] ?: 0.0) + expense.amount)
            
            if (group.algorithmType == AlgorithmType.PERCENTAGE) {
                // In percentage mode, use group-defined percentages for everyone
                var remainingAmount = expense.amount
                group.members.forEachIndexed { index, member ->
                    val splitAmount = if (index == group.members.size - 1) {
                        remainingAmount
                    } else {
                        roundToTwoDecimals(expense.amount * (member.percentage / 100.0))
                    }
                    netBalances[member.id] = roundToTwoDecimals((netBalances[member.id] ?: 0.0) - splitAmount)
                    remainingAmount = roundToTwoDecimals(remainingAmount - splitAmount)
                }
            } else {
                // Each participant is debited their split amount
                for (split in expense.splits) {
                    netBalances[split.memberId] = roundToTwoDecimals((netBalances[split.memberId] ?: 0.0) - split.amount)
                }
            }
        }
        return netBalances
    }


    /**
     * Calculates the total amount spent in the group.
     */
    fun calculateTotalSpent(group: Group): Double {
        return roundToTwoDecimals(group.expenses.sumOf { it.amount })
    }

    private fun calculateBasic(group: Group): List<Transaction> {
        val transactionsMap = mutableMapOf<Pair<String, String>, Double>()
        
        for (expense in group.expenses) {
            val payerId = expense.paidById
            for (split in expense.splits) {
                // Skip if the payer is also the participant (they don't owe themselves)
                if (split.memberId == payerId) continue
                
                // participant owes payer
                val key = split.memberId to payerId
                val amount = split.amount
                
                transactionsMap[key] = (transactionsMap[key] ?: 0.0) + amount
            }
        }
        
        // After aggregating, check for bidirectional debts and simplify them.
        // For example if A owes B 10 and B owes A 2, A should just owe B 8.
        val finalTransactions = mutableListOf<Transaction>()
        val processedKeys = mutableSetOf<Pair<String, String>>()
        
        val keys = transactionsMap.keys.toList()
        for (key in keys) {
            if (key in processedKeys) continue
            
            val reverseKey = key.second to key.first
            val amount = transactionsMap[key] ?: 0.0
            val reverseAmount = transactionsMap[reverseKey] ?: 0.0
            
            val netAmount = roundToTwoDecimals(amount - reverseAmount)
            
            if (netAmount > 0.01) {
                finalTransactions.add(Transaction(from = key.first, to = key.second, amount = netAmount))
            } else if (netAmount < -0.01) {
                finalTransactions.add(Transaction(from = reverseKey.first, to = reverseKey.second, amount = abs(netAmount)))
            }
            
            processedKeys.add(key)
            processedKeys.add(reverseKey)
        }
        
        return finalTransactions
    }

    /**
     * Greedy algorithm to minimize transactions by matching debtors with creditors.
     * This approach doesn't always yield the absolute minimum number of transactions (that's an NP-hard problem),
     * but it provides a very close approximation that's easy to follow.
     */
    private fun calculateSimplified(netBalances: Map<String, Double>): List<Transaction> {
        val debtors = netBalances.filter { it.value < -0.01 }
            .map { it.key to abs(it.value) }
            .sortedByDescending { it.second }
            .toMutableList()
            
        val creditors = netBalances.filter { it.value > 0.01 }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val transactions = mutableListOf<Transaction>()

        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val debtor = debtors[dIdx]
            val creditor = creditors[cIdx]

            val amount = roundToTwoDecimals(minOf(debtor.second, creditor.second))
            
            if (amount > 0.01) {
                transactions.add(Transaction(from = debtor.first, to = creditor.first, amount = amount))
            }

            debtors[dIdx] = debtor.first to roundToTwoDecimals(debtor.second - amount)
            creditors[cIdx] = creditor.first to roundToTwoDecimals(creditors[cIdx].second - amount)

            if (debtors[dIdx].second < 0.01) dIdx++
            if (creditors[cIdx].second < 0.01) cIdx++
        }

        return transactions
    }
}
