package org.oltionzefi.splinge

import android.annotation.SuppressLint
import android.os.Build
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.oltionzefi.splinge.db.DatabaseDriverFactory

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun shareText(text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // URL could not be opened — silently ignore
        }
    }
}

// Application-scoped references; safe to hold as they reference applicationContext only.
@SuppressLint("StaticFieldLeak")
private var platformInstance: AndroidPlatform? = null
private var appContext: Context? = null

fun initPlatform(context: Context) {
    appContext = context.applicationContext
    platformInstance = AndroidPlatform(context.applicationContext)
}

actual fun getPlatform(): Platform = platformInstance
    ?: throw IllegalStateException("Platform not initialized. Call initPlatform(context) first.")

actual fun getDatabaseDriverFactory(): DatabaseDriverFactory =
    DatabaseDriverFactory(appContext
        ?: throw IllegalStateException("Platform not initialized. Call initPlatform(context) first."))
