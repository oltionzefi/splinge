package org.oltionzefi.splinge

import kotlinx.coroutines.CoroutineDispatcher
import org.oltionzefi.splinge.db.DatabaseDriverFactory

interface Platform {
    val name: String
    val ioDispatcher: CoroutineDispatcher
    fun shareText(text: String, title: String)
    fun openUrl(url: String)
}

expect fun getPlatform(): Platform
expect fun getDatabaseDriverFactory(): DatabaseDriverFactory
