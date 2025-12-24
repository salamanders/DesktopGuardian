package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.*
import info.benjaminhill.desktopguardian.parsers.ChromePreferencesParser
import java.io.File

class LinuxSystemMonitor : SystemMonitor {
    override suspend fun getInstalledApps(): List<AppInfo> {
        // As requested: No overhead for app scanning on Linux.
        // Returning empty list means "no apps tracked", so no diffs will occur for apps.
        return emptyList()
    }

    override suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo> {
        return when (browser) {
            BrowserType.CHROME -> scanChromeExtensions()
            else -> emptyList()
        }
    }

    override suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo? {
        return when (browser) {
            BrowserType.CHROME -> scanChromeSearch()
            else -> null
        }
    }

    private fun scanChromeExtensions(): List<ExtensionInfo> {
        val prefsFile = getChromePreferencesFile() ?: return emptyList()
        return try {
            val content = prefsFile.readText()
            ChromePreferencesParser().parse(content, BrowserType.CHROME).extensions
        } catch (e: Exception) {
            println("Failed to parse Chrome preferences on Linux: ${e.message}")
            emptyList()
        }
    }

    private fun scanChromeSearch(): SearchProviderInfo? {
        val prefsFile = getChromePreferencesFile() ?: return null
        return try {
            val content = prefsFile.readText()
            ChromePreferencesParser().parse(content, BrowserType.CHROME).searchProvider
        } catch (e: Exception) {
            println("Failed to parse Chrome preferences on Linux: ${e.message}")
            null
        }
    }

    private fun getChromePreferencesFile(): File? {
        val userHome = System.getProperty("user.home")
        // Standard Chrome config path on Linux
        val possiblePaths = listOf(
            "$userHome/.config/google-chrome/Default/Preferences",
            "$userHome/.config/chromium/Default/Preferences"
        )

        return possiblePaths.map { File(it) }
            .firstOrNull { it.exists() && it.canRead() }
    }
}
