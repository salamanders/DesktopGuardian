package info.benjaminhill.desktopguardian

interface SystemMonitor {
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo>
    suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo?
}
