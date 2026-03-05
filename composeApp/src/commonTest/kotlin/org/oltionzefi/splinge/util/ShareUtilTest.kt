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
        assertEquals(10.0, ShareUtil.roundToTwoDecimals(10.0))
    }

    @Test
    fun round_threeThirds_roundedCorrectly() {
        val result = ShareUtil.roundToTwoDecimals(10.0 / 3.0)
        assertEquals(3.33, result)
    }

    @Test
    fun round_negative_roundedCorrectly() {
        // -1.256 rounds to -1.26 (representable in IEEE 754, no floating-point drift)
        val result = ShareUtil.roundToTwoDecimals(-1.256)
        assertEquals(-1.26, result)
    }

    @Test
    fun round_zero_isZero() {
        assertEquals(0.0, ShareUtil.roundToTwoDecimals(0.0))
    }

    // ── generateReport ─────────────────────────────────────────────────────────

    @Test
    fun generateReport_noExpensesNoTransactions_allSettledMessage() {
        val report = ShareUtil.generateReport(baseGroup(), emptyList())
        assertTrue(report.contains("All settled up!"), "Should show settled message")
        assertTrue(report.contains("No expenses yet."), "Should show no expenses")
        assertTrue(report.contains("Trip"), "Should contain group name")
        assertTrue(report.contains("Splinge"), "Should contain branding")
    }

    @Test
    fun generateReport_withTransactions_listsDebtors() {
        val txns = listOf(
            Transaction(from = "B", to = "A", amount = 15.0),
            Transaction(from = "C", to = "A", amount = 10.0)
        )
        val report = ShareUtil.generateReport(baseGroup(), txns)
        assertTrue(report.contains("Bob"), "Should contain debtor name")
        assertTrue(report.contains("Alice"), "Should contain creditor name")
        assertTrue(report.contains("Carol"))
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
        assertTrue(
            report.contains("DEBT SIMPLIFICATION") || report.contains("BASIC"),
            "Should mention the algorithm used"
        )
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
        assertTrue(report.contains("Settled"), "Should show settled status")
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
    fun generateProfilePaypalLink_correctFormat() {
        val link = ShareUtil.generateProfilePaypalLink("alice")
        assertEquals("https://paypal.me/alice", link)
    }
}

