package org.oltionzefi.splinge

import platform.UIKit.UIDevice
import org.oltionzefi.splinge.db.DatabaseDriverFactory

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun shareText(text: String, title: String) {
        // Sharing requires a UIViewController context; implement via MainViewController if needed.
    }

    override fun openUrl(url: String) {
        // iOS URL opening uses UIApplication.sharedApplication.openURL
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getDatabaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory()
