package org.oltionzefi.splinge.navigation

/**
 * Represents all navigable destinations in the app.
 */
sealed class Screen {
    object GroupList : Screen()
    object GlobalSettings : Screen()
    object CreateGroup : Screen()
    data class GroupDetail(val groupId: String) : Screen()
    data class AddExpense(val groupId: String) : Screen()
    data class AddMember(val groupId: String) : Screen()
    data class GroupSettings(val groupId: String) : Screen()
}

