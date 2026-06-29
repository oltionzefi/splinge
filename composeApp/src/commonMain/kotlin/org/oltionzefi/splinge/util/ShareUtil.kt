package org.oltionzefi.splinge.util

import org.oltionzefi.splinge.model.*

fun roundToTwoDecimals(value: Double): Double {
    val multiplier = 100.0
    return (kotlin.math.round(value * multiplier) / multiplier)
}

fun Double.format(digits: Int): String {
    val rounded = roundToTwoDecimals(this)
    val s = rounded.toString()
    if (!s.contains(".")) return "$s.00"
    val parts = s.split(".")
    val decimal = parts[1].padEnd(digits, '0').take(digits)
    return "${parts[0]}.$decimal"
}

object ShareUtil {


    fun generateReport(
        group: Group,
        transactions: List<Transaction>,
        includeExpenses: Boolean = true,
        expenseRange: IntRange? = null,
        includeOverview: Boolean = true
    ): String {
        val sb = StringBuilder()
        val isLargeList = group.expenses.size > 50

        if (expenseRange != null) {
            sb.append("=== SPLINGE REPORT: PART ${expenseRange.first / 70 + 1} ===\n")
        } else {
            sb.append("=== SPLINGE REPORT ===\n")
        }
        sb.append("Group: ${group.name}\n")
        
        if (includeOverview) {
            if (expenseRange == null) {
                sb.append("Expenses: ${group.expenses.size}\n")
                sb.append("Total: ${group.currency}${org.oltionzefi.splinge.logic.SplitCalculator.calculateTotalSpent(group).format(2)}\n")
            }
            
            sb.append("------------------\n")
            
            if (expenseRange == null) {
                sb.append("SETTLEMENTS:\n")
                if (transactions.isEmpty()) {
                    sb.append("- All settled!\n")
                } else {
                    val grouped = transactions.groupBy { it.to }
                    grouped.forEach { (toMemberId, txs) ->
                        val creditor = group.members.find { it.id == toMemberId }
                        val toName = creditor?.name ?: toMemberId
                        val totalOwed = txs.sumOf { it.amount }
                        sb.append("- $toName is owed ${group.currency}${totalOwed.format(2)}\n")
                        
                        val paypalMe = creditor?.paypalMe
                        if (!paypalMe.isNullOrBlank()) {
                            sb.append("  Pay to $toName: ${generatePaypalLink(paypalMe)}\n")
                        }

                        txs.forEach { trans ->
                            val fromName = group.members.find { it.id == trans.from }?.name ?: trans.from
                            sb.append("  * $fromName pays ${group.currency}${trans.amount.format(2)}\n")
                        }
                    }
                }
            }
        }

        if (includeExpenses) {
            if (includeOverview) {
                sb.append("\nEXPENSES:\n")
            }
            val expensesToDisplay = if (expenseRange != null) {
                group.expenses.slice(expenseRange.coerceIn(group.expenses.indices))
            } else {
                group.expenses
            }

            if (expensesToDisplay.isEmpty()) {
                sb.append("- None\n")
            } else {
                expensesToDisplay.forEachIndexed { index, expense ->
                    val actualIndex = if (expenseRange != null) expenseRange.first + index else index
                    val payer = group.members.find { it.id == expense.paidById }?.name ?: "Unknown"
                    val desc = if (expense.description.length > 35 && isLargeList) {
                        expense.description.take(32) + "..."
                    } else {
                        expense.description
                    }
                    sb.append("${actualIndex + 1}. $desc: ${group.currency}${expense.amount.format(2)} (Paid by $payer)\n")
                }
            }
        }
        
        sb.append("\n------------------\n")
        sb.append("Generated via Splinge")
        
        return sb.toString()
    }

    private fun IntRange.coerceIn(indices: IntRange): IntRange {
        return kotlin.math.max(first, indices.first)..kotlin.math.min(last, indices.last)
    }

    fun generateTransactionReport(group: Group, trans: Transaction): String {
        val fromName = group.members.find { it.id == trans.from }?.name ?: trans.from
        val toName = group.members.find { it.id == trans.to }?.name ?: trans.to
        
        val sb = StringBuilder()
        sb.append("=== PAYMENT REQUEST ===\n")
        sb.append("Group: ${group.name}\n\n")
        
        sb.append("From: $fromName\n")
        sb.append("To: $toName\n")
        sb.append("Amount: ${group.currency}${trans.amount.format(2)}\n")
        sb.append("------------------\n")
        
        sb.append("Hi $fromName, could you please settle your debt of ${group.currency}${trans.amount.format(2)} to $toName for \"${group.name}\"?\n\n")
        
        val paypalMe = group.members.find { it.id == trans.to }?.paypalMe
        if (!paypalMe.isNullOrBlank()) {
            sb.append("You can pay here: ${generatePaypalLink(paypalMe)}\n\n")
        }
        
        sb.append("Sent via Splinge - Split expenses with ease.")
        return sb.toString()
    }

    fun generateExpenseReport(group: Group, expense: Expense): String {
        val payer = group.members.find { it.id == expense.paidById }?.name ?: "Unknown"
        val sb = StringBuilder()
        sb.append("=== EXPENSE DETAILS ===\n")
        sb.append("Description: ${expense.description}\n")
        sb.append("Group: ${group.name}\n")
        sb.append("------------------\n")
        
        sb.append("Amount: ${group.currency}${expense.amount.format(2)}\n")
        sb.append("Paid by: $payer\n\n")
        
        sb.append("Splits breakdown:\n")
        expense.splits.forEach { split ->
            val member = group.members.find { it.id == split.memberId }?.name ?: "Unknown"
            sb.append("- $member: ${group.currency}${split.amount.format(2)}\n")
        }
        
        sb.append("\n------------------\n")
        sb.append("Sent via Splinge")
        return sb.toString()
    }

    fun generateMemberBalanceReport(group: Group, member: Member, balance: Double): String {
        val sb = StringBuilder()
        sb.append("=== MEMBER STATUS ===\n")
        sb.append("Member: ${member.name}\n")
        sb.append("Group: ${group.name}\n")
        sb.append("------------------\n\n")
        
        if (balance > 0.01) {
            sb.append("Current Status: Owed\n")
            sb.append("Balance: ${group.currency}${balance.format(2)}\n")
            sb.append("Good news! You are due to receive money.\n")
        } else if (balance < -0.01) {
            val positiveBalance = if (balance < 0) -balance else balance
            sb.append("Current Status: Owes\n")
            sb.append("Balance: ${group.currency}${positiveBalance.format(2)}\n")
            sb.append("Please consider settling your pending debts.\n")
        } else {
            sb.append("Status: All settled! 0.00\n")
            sb.append("You have no pending balances.\n")
        }
        
        val paypalMe = member.paypalMe
        if (!paypalMe.isNullOrBlank()) {
            sb.append("\nPayment Link: ${generateProfilePaypalLink(paypalMe)}\n")
        }
        
        sb.append("\n------------------\n")
        sb.append("Sent via Splinge")
        return sb.toString()
    }


    fun generatePaypalLink(username: String): String {
        return "https://paypal.me/$username"
    }
    
    fun generateProfilePaypalLink(username: String): String {
        return "https://paypal.me/$username"
    }
}
