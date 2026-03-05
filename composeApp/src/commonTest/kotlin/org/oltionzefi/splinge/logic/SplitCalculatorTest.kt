package org.oltionzefi.splinge.logic

import org.oltionzefi.splinge.model.*
import kotlin.test.*

class SplitCalculatorTest {

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun member(id: String, name: String = id) = Member(id = id, name = name)

    private fun evenSplit(memberIds: List<String>, total: Double): List<Split> {
        val share = total / memberIds.size
        return memberIds.map { Split(memberId = it, amount = shareUtil.roundToTwoDecimals(share)) }
    }

    private val shareUtil = org.oltionzefi.splinge.util.ShareUtil

    private fun group(
        members: List<Member>,
        expenses: List<Expense> = emptyList(),
        algo: AlgorithmType = AlgorithmType.DEBT_SIMPLIFICATION
    ) = Group(
        id = "g1",
        name = "Test Group",
        members = members,
        expenses = expenses,
        algorithmType = algo
    )

    // ── calculateNetBalances ───────────────────────────────────────────────────

    @Test
    fun netBalances_emptyExpenses_allZero() {
        val members = listOf(member("A"), member("B"), member("C"))
        val g = group(members)
        val balances = SplitCalculator.calculateNetBalances(g)
        assertEquals(3, balances.size)
        balances.values.forEach { assertEquals(0.0, it) }
    }

    @Test
    fun netBalances_singleExpenseEvenSplit() {
        // A pays 30, split evenly among A, B, C (10 each)
        val members = listOf(member("A"), member("B"), member("C"))
        val expense = Expense(
            id = "e1", description = "Dinner", amount = 30.0, paidById = "A",
            splits = evenSplit(listOf("A", "B", "C"), 30.0)
        )
        val g = group(members, listOf(expense))
        val balances = SplitCalculator.calculateNetBalances(g)

        // A paid 30 and owes 10 → net +20
        assertEquals(20.0, balances["A"])
        // B owes 10 → net -10
        assertEquals(-10.0, balances["B"])
        // C owes 10 → net -10
        assertEquals(-10.0, balances["C"])
    }

    @Test
    fun netBalances_multipleExpenses() {
        val members = listOf(member("A"), member("B"))
        // A pays 50, split evenly (A=25, B=25)
        val e1 = Expense(
            id = "e1", description = "Groceries", amount = 50.0, paidById = "A",
            splits = evenSplit(listOf("A", "B"), 50.0)
        )
        // B pays 20, split evenly (A=10, B=10)
        val e2 = Expense(
            id = "e2", description = "Coffee", amount = 20.0, paidById = "B",
            splits = evenSplit(listOf("A", "B"), 20.0)
        )
        val g = group(members, listOf(e1, e2))
        val balances = SplitCalculator.calculateNetBalances(g)

        // A: +50 (paid) -25 (own split e1) -10 (split e2) = +15
        assertEquals(15.0, balances["A"])
        // B: +20 (paid) -25 (split e1) -10 (own split e2) = -15
        assertEquals(-15.0, balances["B"])
    }

    // ── BASIC algorithm ────────────────────────────────────────────────────────

    @Test
    fun basic_noExpenses_noTransactions() {
        val g = group(listOf(member("A"), member("B")), algo = AlgorithmType.BASIC)
        assertTrue(SplitCalculator.calculateTransactions(g).isEmpty())
    }

    @Test
    fun basic_singleExpense_correctTransaction() {
        val members = listOf(member("A"), member("B"), member("C"))
        val expense = Expense(
            id = "e1", description = "Lunch", amount = 30.0, paidById = "A",
            splits = evenSplit(listOf("A", "B", "C"), 30.0)
        )
        val g = group(members, listOf(expense), algo = AlgorithmType.BASIC)
        val txns = SplitCalculator.calculateTransactions(g)

        assertEquals(2, txns.size)
        val bToA = txns.find { it.from == "B" && it.to == "A" }
        val cToA = txns.find { it.from == "C" && it.to == "A" }
        assertNotNull(bToA, "B should owe A")
        assertNotNull(cToA, "C should owe A")
        assertEquals(10.0, bToA!!.amount, "B owes 10.0")
        assertEquals(10.0, cToA!!.amount, "C owes 10.0")
    }

    @Test
    fun basic_bidirectionalDebts_simplified() {
        // A pays 10 → B owes 10; B pays 4 → A owes 4 → net B owes A 6
        val members = listOf(member("A"), member("B"))
        val e1 = Expense(
            id = "e1", description = "E1", amount = 10.0, paidById = "A",
            splits = listOf(Split("B", 10.0))
        )
        val e2 = Expense(
            id = "e2", description = "E2", amount = 4.0, paidById = "B",
            splits = listOf(Split("A", 4.0))
        )
        val g = group(members, listOf(e1, e2), algo = AlgorithmType.BASIC)
        val txns = SplitCalculator.calculateTransactions(g)

        assertEquals(1, txns.size)
        assertEquals("B", txns[0].from)
        assertEquals("A", txns[0].to)
        assertEquals(6.0, txns[0].amount)
    }

    @Test
    fun basic_allSettled_noTransactions() {
        // Everyone pays for everyone else, nets to zero
        val members = listOf(member("A"), member("B"))
        val e1 = Expense(
            id = "e1", description = "E1", amount = 10.0, paidById = "A",
            splits = listOf(Split("B", 10.0))
        )
        val e2 = Expense(
            id = "e2", description = "E2", amount = 10.0, paidById = "B",
            splits = listOf(Split("A", 10.0))
        )
        val g = group(members, listOf(e1, e2), algo = AlgorithmType.BASIC)
        val txns = SplitCalculator.calculateTransactions(g)
        assertTrue(txns.isEmpty(), "Fully settled groups should produce no transactions")
    }

    // ── DEBT_SIMPLIFICATION algorithm ─────────────────────────────────────────

    @Test
    fun debtSimplification_noExpenses_noTransactions() {
        val g = group(listOf(member("A"), member("B")))
        assertTrue(SplitCalculator.calculateTransactions(g).isEmpty())
    }

    @Test
    fun debtSimplification_threePersonChain_minimized() {
        // A pays 30; B owes 10, C owes 10, A owes 10
        // Net: A = +20, B = -10, C = -10
        // Simplified: B→A 10, C→A 10
        val members = listOf(member("A"), member("B"), member("C"))
        val expense = Expense(
            id = "e1", description = "Dinner", amount = 30.0, paidById = "A",
            splits = evenSplit(listOf("A", "B", "C"), 30.0)
        )
        val g = group(members, listOf(expense))
        val txns = SplitCalculator.calculateTransactions(g)

        assertEquals(2, txns.size)
        val amounts = txns.map { it.amount }.sorted()
        assertEquals(listOf(10.0, 10.0), amounts)
        txns.forEach { assertEquals("A", it.to) }
    }

    @Test
    fun debtSimplification_complexGroup_balancedToZero() {
        // Any algorithm must leave everyone's net balance at 0 after applying transactions
        val members = listOf(member("A"), member("B"), member("C"), member("D"))
        val expenses = listOf(
            Expense("e1", "Rent", 120.0, "A", evenSplit(listOf("A", "B", "C", "D"), 120.0)),
            Expense("e2", "Food", 40.0, "B", evenSplit(listOf("A", "B", "C", "D"), 40.0)),
            Expense("e3", "Transport", 20.0, "C", evenSplit(listOf("A", "B", "D"), 20.0))
        )
        val g = group(members, expenses)
        val txns = SplitCalculator.calculateTransactions(g)

        // After applying all transactions, net for each member should be ~0
        val net = members.associate { it.id to 0.0 }.toMutableMap()
        SplitCalculator.calculateNetBalances(g).forEach { (id, balance) -> net[id] = balance }

        txns.forEach { t ->
            net[t.from] = shareUtil.roundToTwoDecimals((net[t.from] ?: 0.0) + t.amount)
            net[t.to] = shareUtil.roundToTwoDecimals((net[t.to] ?: 0.0) - t.amount)
        }

        net.forEach { (id, balance) ->
            assertTrue(kotlin.math.abs(balance) < 0.02, "Member $id should be settled, but balance is $balance")
        }
    }

    @Test
    fun debtSimplification_singlePayer_allOwe() {
        val members = listOf(member("A"), member("B"), member("C"))
        // B and C owe A everything (A not in splits)
        val expense = Expense(
            id = "e1", description = "Taxi", amount = 40.0, paidById = "A",
            splits = listOf(Split("B", 20.0), Split("C", 20.0))
        )
        val g = group(members, listOf(expense))
        val txns = SplitCalculator.calculateTransactions(g)

        assertEquals(2, txns.size)
        txns.forEach {
            assertEquals("A", it.to)
            assertEquals(20.0, it.amount)
        }
        val froms = txns.map { it.from }.toSet()
        assertEquals(setOf("B", "C"), froms)
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    @Test
    fun amounts_areRoundedToTwoDecimals() {
        // 10 / 3 = 3.333… — ensure no floating-point leakage
        val members = listOf(member("A"), member("B"), member("C"))
        val share = shareUtil.roundToTwoDecimals(10.0 / 3.0)
        val expense = Expense(
            id = "e1", description = "Split", amount = 10.0, paidById = "A",
            splits = listOf(Split("A", share), Split("B", share), Split("C", share))
        )
        val g = group(members, listOf(expense))
        val txns = SplitCalculator.calculateTransactions(g)
        txns.forEach { txn ->
            val rounded = shareUtil.roundToTwoDecimals(txn.amount)
            assertEquals(rounded, txn.amount, "Transaction amount ${txn.amount} should be rounded to 2 decimals")
        }
    }

    @Test
    fun payer_notChargedForOwnExpense() {
        // A pays 30 but only B and C are in splits → A is purely a creditor
        val members = listOf(member("A"), member("B"), member("C"))
        val expense = Expense(
            id = "e1", description = "Hotel", amount = 30.0, paidById = "A",
            splits = listOf(Split("B", 15.0), Split("C", 15.0))
        )
        val g = group(members, listOf(expense), algo = AlgorithmType.BASIC)
        val txns = SplitCalculator.calculateTransactions(g)

        txns.forEach { assertNotEquals("A", it.from, "A should never be a debtor here") }
    }
}

