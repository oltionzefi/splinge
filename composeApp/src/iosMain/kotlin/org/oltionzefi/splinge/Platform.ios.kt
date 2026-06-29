package org.oltionzefi.splinge

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.UIKit.UIDevice
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.UIKit.UIViewController
import org.oltionzefi.splinge.db.DatabaseDriverFactory

private var rootViewController: UIViewController? = null

fun setRootViewController(controller: UIViewController) {
    rootViewController = controller
}

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun shareText(text: String, title: String) {
        val currentViewController = rootViewController ?: UIApplication.sharedApplication.keyWindow?.rootViewController
        if (currentViewController != null) {
            val activityViewController = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null
            )
            currentViewController.presentViewController(activityViewController, animated = true, completion = null)
        }
    }

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getDatabaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory()
