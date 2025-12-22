package info.benjaminhill.desktopguardian.monitor

import info.benjaminhill.desktopguardian.model.AppInfo
import info.benjaminhill.desktopguardian.model.ExtensionInfo
import info.benjaminhill.desktopguardian.model.SearchProviderInfo
import info.benjaminhill.desktopguardian.model.BrowserType

interface SystemMonitor {
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo>
    suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo
}
