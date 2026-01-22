package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import java.io.File

class LinuxSystemMonitor : OsQuerySystemMonitor() {

    override suspend fun getInstalledApps(): List<AppInfo> {
        // App scanning on Linux remains disabled as per original logic/requests
        return emptyList()
    }

    override fun getPreferencesFile(browser: BrowserType): File? {
        val userHome = System.getProperty("user.home")
        val possiblePaths =
            listOf(
                "$userHome/.config/google-chrome/Default/Preferences",
                "$userHome/.config/chromium/Default/Preferences",
            )

        return possiblePaths.map { File(it) }
            .firstOrNull { it.exists() && it.canRead() }
    }
}
