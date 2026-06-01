package org.oltionzefi.splinge.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.oltionzefi.splinge.model.*

/**
 * Single source of truth for all persistent app data.
 * All reads are reactive (Flow); all writes are suspending functions.
 */
class AppRepository(driverFactory: DatabaseDriverFactory, private val ioDispatcher: CoroutineDispatcher) {

    private val driver = driverFactory.createDriver()
    private val database = SplingeDatabase(driver)
    private val groupQ = database.groupQueries
    private val memberQ = database.memberQueries
    private val expenseQ = database.expenseQueries
    private val splitQ = database.splitQueries
    private val settingsQ = database.settingsQueries
    
    init {
        // Run safe migrations for existing databases that might be missing new columns.
        // These are wrapped in try-catch because ALTER TABLE fails if the column already exists.
        val migrations = listOf(
            "ALTER TABLE GroupEntity ADD COLUMN algorithmType TEXT NOT NULL DEFAULT 'DEBT_SIMPLIFICATION';",
            "ALTER TABLE GroupEntity ADD COLUMN currency TEXT NOT NULL DEFAULT '€';",
            "ALTER TABLE MemberEntity ADD COLUMN percentage REAL NOT NULL DEFAULT 0.0;"
        )

        migrations.forEach { sql ->
            try {
                driver.execute(null, sql, 0)
            } catch (e: Exception) {
                // Column likely already exists or table doesn't exist yet
            }
        }
    }

    // ── Groups ────────────────────────────────────────────────────────────────
    
    private fun safeAlgorithmType(value: String): AlgorithmType {
        return try {
            AlgorithmType.valueOf(value)
        } catch (e: Exception) {
            AlgorithmType.DEBT_SIMPLIFICATION
        }
    }

    /** Emits the full list of [Group]s whenever the DB changes. */
    val groups: Flow<List<Group>> =
        groupQ.selectAllGroups().asFlow().mapToList(ioDispatcher).map { rows ->
            try {
                rows.map { row ->
                    val members = memberQ.selectMembersByGroup(row.id).executeAsList().map { m ->
                        Member(id = m.id, name = m.name, paypalMe = m.paypalMe, percentage = m.percentage)
                    }
                    val expenses = expenseQ.selectExpensesByGroup(row.id).executeAsList().map { e ->
                        val splits = splitQ.selectSplitsByExpense(e.id, e.groupId).executeAsList().map { s ->
                            Split(memberId = s.memberId, amount = s.amount)
                        }
                        Expense(
                            id = e.id,
                            description = e.description,
                            amount = e.amount,
                            paidById = e.paidById,
                            splits = splits,
                            date = e.date
                        )
                    }
                    Group(
                        id = row.id,
                        name = row.name,
                        members = members,
                        expenses = expenses,
                        algorithmType = safeAlgorithmType(row.algorithmType),
                        currency = row.currency
                    )
                }
            } catch (e: Exception) {
                // If anything fails during mapping (e.g. missing columns), return empty list
                emptyList()
            }
        }.catch { 
            // Handle query execution errors (e.g. schema mismatch before migration completes)
            emit(emptyList()) 
        }

    suspend fun saveGroup(group: Group) = withContext(ioDispatcher) {
        database.transaction {
            groupQ.insertGroup(group.id, group.name, group.algorithmType.name, group.currency)
            memberQ.deleteMembersByGroup(group.id)
            group.members.forEach { m ->
                memberQ.insertMember(m.id, group.id, m.name, m.paypalMe, m.percentage)
            }
            expenseQ.deleteExpensesByGroup(group.id)
            splitQ.deleteSplitsByGroup(group.id)
            group.expenses.forEach { e ->
                expenseQ.insertExpense(e.id, group.id, e.description, e.amount, e.paidById, e.date)
                e.splits.forEach { s ->
                    splitQ.insertSplit(e.id, group.id, s.memberId, s.amount)
                }
            }
        }
    }

    suspend fun deleteGroup(groupId: String) = withContext(ioDispatcher) {
        database.transaction {
            splitQ.deleteSplitsByGroup(groupId)
            expenseQ.deleteExpensesByGroup(groupId)
            memberQ.deleteMembersByGroup(groupId)
            groupQ.deleteGroup(groupId)
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun loadSettings(): UserSettings {
        return try {
            val row = settingsQ.selectSettings().executeAsOneOrNull()
            if (row != null) {
                UserSettings(
                    name = row.userName,
                    paypalMe = row.paypalMe,
                    isSeeded = row.isSeeded == 1L
                )
            } else {
                UserSettings()
            }
        } catch (e: Exception) {
            // This might happen if the table schema changed but was not migrated
            UserSettings()
        }
    }

    suspend fun saveSettings(settings: UserSettings) = withContext(ioDispatcher) {
        try {
            settingsQ.upsertSettings(
                userName = settings.name,
                paypalMe = settings.paypalMe,
                isSeeded = if (settings.isSeeded) 1L else 0L
            )
        } catch (e: Exception) {
            // Ignore failure to save settings if DB schema is incompatible
        }
    }
}
