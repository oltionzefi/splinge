package org.oltionzefi.splinge.db

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific factory that creates the SQLDelight SqlDriver. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

