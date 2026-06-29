package org.oltionzefi.splinge.util

import org.oltionzefi.splinge.model.*
import kotlin.test.*

class ShareUtilTest {

    // ── Helpers ────────────────────────────────────────────────────────────────

    private val alice = Member(id = "A", name = "Alice", paypalMe = "alice")
    private val bob = Member(id = "B", name = "Bob", paypalMe = "bob")
    private val carol = Member(id = "C", name = "Carol")

    private fun baseGroup(
        members: List<Member> = listOf(alice, bob, carol),
        expenses: List<Expense> = emptyList()
    ) = Group(
        id = "g1", name = "Trip", members = members, expenses = expenses
    )

    // ── roundToTwoDecimals ─────────────────────────────────────────────────────

    @Test
    fun round_exactValue_unchanged() {
        assertEquals(10.0, roundToTwoDecimals(10.0))
    }

    @Test
    fun round_threeThirds_roundedCorrectly() {
        val result = roundToTwoDecimals(10.0 / 3.0)
        assertEquals(3.33, result)
    }

    @Test
    fun round_negative_roundedCorrectly() {
        // -1.256 rounds to -1.26 (representable in IEEE 754, no floating-point drift)
        val result = roundToTwoDecimals(-1.256)
        assertEquals(-1.26, result)
    }

    @Test
    fun round_zero_isZero() {
        assertEquals(0.0, roundToTwoDecimals(0.0))
    }

    // ── generateReport ─────────────────────────────────────────────────────────

    @Test
    fun generateReport_containsTotalSpent() {
        val expense = Expense(
            id = "e1", description = "Hotel", amount = 120.0, paidById = "A",
            splits = emptyList()
        )
        val g = baseGroup(expenses = listOf(expense))
        val report = ShareUtil.generateReport(g, emptyList())
        assertTrue(report.contains("Total: €120.00"), "Should show total spent with currency")
    }

    @Test
    fun generateReport_noExpensesNoTransactions_allSettledMessage() {
        val report = ShareUtil.generateReport(baseGroup(), emptyList())
        assertTrue(report.contains("All settled!"), "Should show settled message")
        assertTrue(report.contains("EXPENSES:"), "Should show expenses section")
        assertTrue(report.contains("- None"), "Should show no expenses")
        assertTrue(report.contains("Trip"), "Should contain group name")
        assertTrue(report.contains("Splinge"), "Should contain branding")
    }

    @Test
    fun generateReport_withTransactions_listsDebtorsGrouped() {
        val txns = listOf(
            Transaction(from = "B", to = "A", amount = 15.0),
            Transaction(from = "C", to = "A", amount = 10.0)
        )
        val report = ShareUtil.generateReport(baseGroup(), txns)
        assertTrue(report.contains("Alice is owed €25.00"), "Should show total owed to Alice")
        assertTrue(report.contains("Bob pays €15.00"), "Should show individual debt from Bob")
        assertTrue(report.contains("Carol pays €10.00"), "Should show individual debt from Carol")
    }

    @Test
    fun generateReport_withExpenses_listsExpenseDescriptions() {
        val expense = Expense(
            id = "e1", description = "Hotel", amount = 120.0, paidById = "A",
            splits = listOf(Split("B", 60.0), Split("C", 60.0))
        )
        val g = baseGroup(expenses = listOf(expense))
        val report = ShareUtil.generateReport(g, emptyList())
        assertTrue(report.contains("Hotel"), "Should contain expense description")
        assertTrue(report.contains("Alice"), "Should show payer name")
        assertTrue(report.contains("120"), "Should show expense amount")
    }

    @Test
    fun generateReport_containsAlgorithmType() {
        val report = ShareUtil.generateReport(baseGroup(), emptyList())
        assertTrue(report.contains("Generated via Splinge"))
    }

    @Test
    fun generateReport_500elements_isNotTruncated() {
        val members = listOf(alice, bob, carol)
        val expenses = (1..500).map { i ->
            Expense(id = "e$i", description = "Expense $i", amount = 10.0, paidById = "A", splits = emptyList())
        }
        val group = Group(id = "g1", name = "Large Group", members = members, expenses = expenses)
        val report = ShareUtil.generateReport(group, emptyList())

        assertFalse(report.contains("Showing latest"), "Report should not be truncated")
        assertTrue(report.contains("1. Expense 1:"), "Should contain the first expense")
        assertTrue(report.contains("500. Expense 500:"), "Should contain the 500th expense")
        assertTrue(report.contains("(Paid by Alice)"), "Should contain the full 'Paid by' label")
    }

    @Test
    fun generateReport_hugeList_isNotTruncated() {
        val members = (1..3).map { Member(it.toString(), "M$it", "p$it") }
        val expenses = (1..600).map { i ->
            Expense(i.toString(), "E$i", 10.0, "1", emptyList())
        }
        val group = Group("g1", "Huge", members, expenses = expenses)
        val report = ShareUtil.generateReport(group, emptyList())
        
        assertFalse(report.contains("Showing latest"), "Should NOT show truncation message")
        assertTrue(report.contains("600. E600"), "Should contain the last item")
        assertTrue(report.contains("1. E1:"), "Should contain the first item")
    }

    @Test
    fun generateReport_largeList_isNotTruncatedAbove500() {
        val expenses = (1..550).map { i ->
            Expense(id = "e$i", description = "Exp $i", amount = 1.0, paidById = "A", splits = emptyList())
        }
        val g = baseGroup(expenses = expenses)
        val report = ShareUtil.generateReport(g, emptyList())
        
        assertFalse(report.contains("Showing latest"), "Should NOT show truncation message")
        assertTrue(report.contains("1. Exp 1:"), "Should contain the first item")
        assertTrue(report.contains("550. Exp 550:"), "Should contain the last expense")
    }

    // ── generateTransactionReport ──────────────────────────────────────────────

    @Test
    fun generateTransactionReport_containsFromAndToNames() {
        val txn = Transaction(from = "B", to = "A", amount = 25.0)
        val report = ShareUtil.generateTransactionReport(baseGroup(), txn)
        assertTrue(report.contains("Bob"), "Should contain payer name")
        assertTrue(report.contains("Alice"), "Should contain recipient name")
        assertTrue(report.contains("25"), "Should contain amount")
    }

    @Test
    fun generateTransactionReport_withPaypalMe_includesLink() {
        val txn = Transaction(from = "B", to = "A", amount = 10.0)
        val report = ShareUtil.generateTransactionReport(baseGroup(), txn)
        assertTrue(report.contains("paypal.me/alice"), "Should include PayPal link for Alice")
        assertTrue(report.contains("https://"), "Should include https protocol")
        assertFalse(report.contains("/10"), "Should not include amount in link")
    }

    @Test
    fun generateTransactionReport_withoutPaypalMe_noLink() {
        // Carol has no paypalMe
        val members = listOf(alice, bob, carol)
        val txn = Transaction(from = "B", to = "C", amount = 10.0)
        val report = ShareUtil.generateTransactionReport(baseGroup(members = members), txn)
        assertFalse(report.contains("paypal.me/carol"), "Carol has no paypal link")
    }

    // ── generateExpenseReport ──────────────────────────────────────────────────

    @Test
    fun generateExpenseReport_containsExpenseDetails() {
        val expense = Expense(
            id = "e1", description = "Dinner", amount = 90.0, paidById = "A",
            splits = listOf(Split("B", 45.0), Split("C", 45.0))
        )
        val report = ShareUtil.generateExpenseReport(baseGroup(), expense)
        assertTrue(report.contains("Dinner"))
        assertTrue(report.contains("Alice"), "Should show payer name")
        assertTrue(report.contains("90"), "Should show total")
        assertTrue(report.contains("Bob"))
        assertTrue(report.contains("Carol"))
        assertTrue(report.contains("45"))
    }

    @Test
    fun generateExpenseReport_unknownPayer_showsUnknown() {
        val expense = Expense(
            id = "e1", description = "Misc", amount = 10.0, paidById = "GHOST",
            splits = emptyList()
        )
        val report = ShareUtil.generateExpenseReport(baseGroup(), expense)
        assertTrue(report.contains("Unknown"), "Unknown payer should display as Unknown")
    }

    // ── generateMemberBalanceReport ────────────────────────────────────────────

    @Test
    fun memberBalanceReport_owedMoney_showsOwed() {
        val report = ShareUtil.generateMemberBalanceReport(baseGroup(), alice, 20.0)
        assertTrue(report.contains("Owed"), "Should say 'Owed'")
        assertTrue(report.contains("Alice"))
    }

    @Test
    fun memberBalanceReport_owesMoney_showsOwes() {
        val report = ShareUtil.generateMemberBalanceReport(baseGroup(), bob, -15.0)
        assertTrue(report.contains("Owes"), "Should say 'Owes'")
        assertTrue(report.contains("15"))
    }

    @Test
    fun memberBalanceReport_settled_showsSettled() {
        val report = ShareUtil.generateMemberBalanceReport(baseGroup(), alice, 0.0)
        assertTrue(report.contains("settled"), "Should show settled status")
    }

    @Test
    fun memberBalanceReport_withPaypalMe_includesLink() {
        val report = ShareUtil.generateMemberBalanceReport(baseGroup(), alice, 10.0)
        assertTrue(report.contains("paypal.me/alice"), "Should include PayPal profile link")
    }

    @Test
    fun memberBalanceReport_noPaypalMe_noLink() {
        val report = ShareUtil.generateMemberBalanceReport(baseGroup(), carol, 5.0)
        assertFalse(report.contains("paypal.me"), "Carol should not have a PayPal link")
    }

    // ── generateProfilePaypalLink ──────────────────────────────────────────────

    @Test
    fun generateReport_withIncludeExpensesFalse_noExpensesSection() {
        val expense = Expense(id = "e1", description = "Hotel", amount = 10.0, paidById = "A", splits = emptyList())
        val g = baseGroup(expenses = listOf(expense))
        val report = ShareUtil.generateReport(g, emptyList(), includeExpenses = false)
        assertFalse(report.contains("EXPENSES:"), "Should NOT show expenses section")
        assertFalse(report.contains("Hotel"), "Should NOT contain expense details")
    }

    @Test
    fun generateReport_withIncludeOverviewFalse_noOverview() {
        val expense = Expense(id = "e1", description = "Hotel", amount = 10.0, paidById = "A", splits = emptyList())
        val g = baseGroup(expenses = listOf(expense))
        val report = ShareUtil.generateReport(g, emptyList(), includeExpenses = true, includeOverview = false)
        assertFalse(report.contains("SETTLEMENTS:"), "Should NOT show settlements")
        assertFalse(report.contains("Total:"), "Should NOT show total")
        assertTrue(report.contains("Hotel"), "Should contain expense details")
    }

    @Test
    fun generateReport_withExpenseRange_showsOnlyRange() {
        val expenses = (1..100).map { i ->
            Expense(id = "e$i", description = "Exp $i", amount = 1.0, paidById = "A", splits = emptyList())
        }
        val g = baseGroup(expenses = expenses)
        val report = ShareUtil.generateReport(g, emptyList(), includeExpenses = true, expenseRange = 0..69, includeOverview = false)
        
        assertTrue(report.contains("PART 1"), "Should mention PART 1")
        assertTrue(report.contains("1. Exp 1:"), "Should contain the first expense")
        assertTrue(report.contains("70. Exp 70:"), "Should contain the 70th expense")
        assertFalse(report.contains("71. Exp 71:"), "Should NOT contain the 71st expense")
        assertFalse(report.contains("SETTLEMENTS:"), "Expense-only part should NOT contain settlements")
    }

    @Test
    fun generateReport_withExpenseRangePart2_correctIndexing() {
        val expenses = (1..100).map { i ->
            Expense(id = "e$i", description = "Exp $i", amount = 1.0, paidById = "A", splits = emptyList())
        }
        val g = baseGroup(expenses = expenses)
        val report = ShareUtil.generateReport(g, emptyList(), includeExpenses = true, expenseRange = 70..139, includeOverview = false)
        
        assertTrue(report.contains("PART 2"), "Should mention PART 2")
        assertTrue(report.contains("71. Exp 71:"), "Should contain the 71st expense (1-indexed)")
        assertTrue(report.contains("100. Exp 100:"), "Should contain the 100th expense")
        assertFalse(report.contains("1. Exp 1:"), "Should NOT contain the first expense")
    }
}

