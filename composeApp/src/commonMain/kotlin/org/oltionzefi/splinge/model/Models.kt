package org.oltionzefi.splinge.model

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: String,
    val name: String,
    val paypalMe: String? = null
)

@Serializable
data class Expense(
    val id: String,
    val description: String,
    val amount: Double,
    val paidById: String,
    val splits: List<Split>,
    val date: Long = 0L // Placeholder for simplicity
)

@Serializable
data class Split(
    val memberId: String,
    val amount: Double
)

@Serializable
enum class AlgorithmType {
    BASIC, // Everyone owes the payer
    DEBT_SIMPLIFICATION // Minimize total number of transactions
}

@Serializable
data class Group(
    val id: String,
    val name: String,
    val members: List<Member>,
    val expenses: List<Expense> = emptyList(),
    val algorithmType: AlgorithmType = AlgorithmType.DEBT_SIMPLIFICATION,
    val currency: String = "€"
)

data class Transaction(
    val from: String,
    val to: String,
    val amount: Double
)

@Serializable
data class UserSettings(
    val name: String = "",
    val paypalMe: String = ""
)
