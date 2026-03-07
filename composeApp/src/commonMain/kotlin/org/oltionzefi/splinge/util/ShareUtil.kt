package org.oltionzefi.splinge.util

import org.oltionzefi.splinge.model.*

object ShareUtil {

    fun generateReport(group: Group, transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("📋 Splinge Report: ${group.name}\n")
        sb.append("--------------------------------\n\n")
        
        sb.append("💰 Balances:\n")
        if (transactions.isEmpty()) {
            sb.append("All settled up! 🎉\n")
        } else {
            transactions.forEach { trans ->
                sb.append(generateTransactionReportLine(group, trans))
            }
        }
        sb.append("\n")

        sb.append("📝 Expenses:\n")
        if (group.expenses.isEmpty()) {
            sb.append("No expenses yet.\n")
        } else {
            group.expenses.forEach { expense ->
                val payer = group.members.find { it.id == expense.paidById }?.name ?: "Unknown"
                sb.append("- ${expense.description}: ${group.currency}${expense.amount.format(2)} (Paid by $payer)\n")
            }
        }
        sb.append("\n")
        
        sb.append("⚙️ Method used: ${group.algorithmType.name.replace("_", " ")}\n")
        sb.append("--------------------------------\n")
        sb.append("Sent via Splinge")
        
        return sb.toString()
    }

    fun generateTransactionReport(group: Group, trans: Transaction): String {
        val fromName = group.members.find { it.id == trans.from }?.name ?: trans.from
        val toName = group.members.find { it.id == trans.to }?.name ?: trans.to
        
        val sb = StringBuilder()
        sb.append("💸 Splinge Payment Request\n")
        sb.append("--------------------------------\n")
        sb.append("$fromName owes $toName: ${group.currency}${trans.amount.format(2)}\n")
        
        val paypalMe = group.members.find { it.id == trans.to }?.paypalMe
        if (!paypalMe.isNullOrBlank()) {
            val link = generatePaypalLink(paypalMe)
            sb.append("\n🚀 Pay here: $link\n")
        }
        sb.append("--------------------------------\n")
        sb.append("Sent via Splinge")
        return sb.toString()
    }

    fun generateExpenseReport(group: Group, expense: Expense): String {
        val payer = group.members.find { it.id == expense.paidById }?.name ?: "Unknown"
        val sb = StringBuilder()
        sb.append("📝 Splinge Expense Details\n")
        sb.append("--------------------------------\n")
        sb.append("Description: ${expense.description}\n")
        sb.append("Total: ${group.currency}${expense.amount.format(2)}\n")
        sb.append("Paid by: $payer\n\n")
        
        sb.append("Splits:\n")
        expense.splits.forEach { split ->
            val member = group.members.find { it.id == split.memberId }?.name ?: "Unknown"
            sb.append("- $member: ${group.currency}${split.amount.format(2)}\n")
        }
        sb.append("--------------------------------\n")
        sb.append("Sent via Splinge")
        return sb.toString()
    }

    fun generateMemberBalanceReport(group: Group, member: Member, balance: Double): String {
        val sb = StringBuilder()
        sb.append("👤 Splinge Member Balance\n")
        sb.append("--------------------------------\n")
        sb.append("Member: ${member.name}\n")
        if (balance > 0.01) {
            sb.append("Status: Owed ${group.currency}${balance.format(2)}\n")
        } else if (balance < -0.01) {
            val positiveBalance = if (balance < 0) -balance else balance
            sb.append("Status: Owes ${group.currency}${positiveBalance.format(2)}\n")
        } else {
            sb.append("Status: Settled up! 🎉\n")
        }
        
        if (!member.paypalMe.isNullOrBlank()) {
            val link = generateProfilePaypalLink(member.paypalMe)
            sb.append("\n🚀 Pay here: $link\n")
        }
        sb.append("--------------------------------\n")
        sb.append("Sent via Splinge")
        return sb.toString()
    }

    private fun generateTransactionReportLine(group: Group, trans: Transaction): String {
        val fromName = group.members.find { it.id == trans.from }?.name ?: trans.from
        val toName = group.members.find { it.id == trans.to }?.name ?: trans.to
        
        return "- $fromName owes $toName: ${group.currency}${trans.amount.format(2)}\n"
    }

    fun generatePaypalLink(username: String): String {
        return "paypal.me/$username"
    }
    
    fun generateProfilePaypalLink(username: String): String {
        return "paypal.me/$username"
    }

    // Rounding helper
    fun roundToTwoDecimals(value: Double): Double {
        val multiplier = 100.0
        return (kotlin.math.round(value * multiplier) / multiplier)
    }

    // Simple format extension (Multiplatform-safe enough for this)
    private fun Double.format(digits: Int): String {
        return roundToTwoDecimals(this).toString()
    }
}
