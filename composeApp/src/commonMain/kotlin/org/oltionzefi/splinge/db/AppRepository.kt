package org.oltionzefi.splinge.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.oltionzefi.splinge.model.*

/**
 * Single source of truth for all persistent app data.
 * All reads are reactive (Flow); all writes are suspending functions.
 */
class AppRepository(driverFactory: DatabaseDriverFactory) {

    private val database = SplingeDatabase(driverFactory.createDriver())
    private val groupQ = database.groupQueries
    private val memberQ = database.memberQueries
    private val expenseQ = database.expenseQueries
    private val splitQ = database.splitQueries
    private val settingsQ = database.settingsQueries

    // ── Groups ────────────────────────────────────────────────────────────────

    /** Emits the full list of [Group]s whenever the DB changes. */
    val groups: Flow<List<Group>> =
        groupQ.selectAllGroups().asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { row ->
                val members = memberQ.selectMembersByGroup(row.id).executeAsList().map { m ->
                    Member(id = m.id, name = m.name, paypalMe = m.paypalMe)
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
                    algorithmType = AlgorithmType.valueOf(row.algorithmType),
                    currency = row.currency
                )
            }
        }

    suspend fun saveGroup(group: Group) = withContext(Dispatchers.IO) {
        database.transaction {
            groupQ.insertGroup(group.id, group.name, group.algorithmType.name, group.currency)
            memberQ.deleteMembersByGroup(group.id)
            group.members.forEach { m ->
                memberQ.insertMember(m.id, group.id, m.name, m.paypalMe)
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

    suspend fun deleteGroup(groupId: String) = withContext(Dispatchers.IO) {
        database.transaction {
            splitQ.deleteSplitsByGroup(groupId)
            expenseQ.deleteExpensesByGroup(groupId)
            memberQ.deleteMembersByGroup(groupId)
            groupQ.deleteGroup(groupId)
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun loadSettings(): UserSettings {
        val row = settingsQ.selectSettings().executeAsOneOrNull()
        return if (row != null) UserSettings(name = row.userName, paypalMe = row.paypalMe)
        else UserSettings()
    }

    suspend fun saveSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        settingsQ.upsertSettings(settings.name, settings.paypalMe)
    }
}
