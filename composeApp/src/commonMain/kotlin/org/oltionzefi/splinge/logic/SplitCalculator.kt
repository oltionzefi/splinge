package org.oltionzefi.splinge.logic

import org.oltionzefi.splinge.model.*
import org.oltionzefi.splinge.util.ShareUtil
import kotlin.math.abs
import kotlin.math.round

object SplitCalculator {

    /**
     * Calculates the minimum set of transactions required to settle all debts within a group.
     * Supports two algorithms:
     * - BASIC: Aggregates all expenses and simplifies bidirectional debts.
     * - DEBT_SIMPLIFICATION: Uses a greedy algorithm to minimize the total number of transactions.
     */
    fun calculateTransactions(group: Group): List<Transaction> {
        return when (group.algorithmType) {
            AlgorithmType.BASIC -> calculateBasic(group)
            AlgorithmType.DEBT_SIMPLIFICATION -> {
                val netBalances = calculateNetBalances(group)
                calculateSimplified(netBalances)
            }
        }
    }

    /**
     * Calculates the net balance for each member in the group.
     * A positive balance means the member is owed money, while a negative balance means they owe money.
     */
    fun calculateNetBalances(group: Group): Map<String, Double> {
        val netBalances = mutableMapOf<String, Double>()
        
        // Initialize balances
        group.members.forEach { netBalances[it.id] = 0.0 }
        
        // Calculate net balance for each member
        for (expense in group.expenses) {
            // Payer is credited the full amount
            netBalances[expense.paidById] = ShareUtil.roundToTwoDecimals((netBalances[expense.paidById] ?: 0.0) + expense.amount)
            
            // Each participant is debited their split amount
            for (split in expense.splits) {
                netBalances[split.memberId] = ShareUtil.roundToTwoDecimals((netBalances[split.memberId] ?: 0.0) - split.amount)
            }
        }
        return netBalances
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
            
            val netAmount = ShareUtil.roundToTwoDecimals(amount - reverseAmount)
            
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

            val amount = ShareUtil.roundToTwoDecimals(minOf(debtor.second, creditor.second))
            
            if (amount > 0.01) {
                transactions.add(Transaction(from = debtor.first, to = creditor.first, amount = amount))
            }

            debtors[dIdx] = debtor.first to ShareUtil.roundToTwoDecimals(debtor.second - amount)
            creditors[cIdx] = creditor.first to ShareUtil.roundToTwoDecimals(creditors[cIdx].second - amount)

            if (debtors[dIdx].second < 0.01) dIdx++
            if (creditors[cIdx].second < 0.01) cIdx++
        }

        return transactions
    }
}
