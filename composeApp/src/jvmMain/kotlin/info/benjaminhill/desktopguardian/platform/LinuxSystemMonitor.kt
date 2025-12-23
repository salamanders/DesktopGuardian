package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import info.benjaminhill.desktopguardian.ExtensionInfo
import info.benjaminhill.desktopguardian.SearchProviderInfo
import info.benjaminhill.desktopguardian.SystemMonitor

class LinuxSystemMonitor : SystemMonitor {
    override suspend fun getInstalledApps(): List<AppInfo> {
        // Return a dummy list to prove it works in the UI
        return listOf(
            AppInfo("Linux Test App", "1.0", System.currentTimeMillis())
        )
    }

    override suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo> {
        return emptyList()
    }

    override suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo? {
        return null
    }
}
