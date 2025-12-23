package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import info.benjaminhill.desktopguardian.ExtensionInfo
import info.benjaminhill.desktopguardian.SearchProviderInfo
import info.benjaminhill.desktopguardian.SystemMonitor
import info.benjaminhill.desktopguardian.parsers.ChromePreferencesParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class MacOsSystemMonitor : SystemMonitor {

    private val chromeParser = ChromePreferencesParser()

    override suspend fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val searchPaths = listOf(
            Paths.get("/Applications"),
            Paths.get(System.getProperty("user.home"), "Applications")
        )

        for (path in searchPaths) {
            if (Files.exists(path)) {
                try {
                    Files.walk(path, 2).use { stream ->
                        stream.filter { it.toString().endsWith(".app") }
                            .forEach { appPath ->
                                val name = appPath.fileName.toString().removeSuffix(".app")
                                apps.add(AppInfo(name, null, 0L))
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return apps
    }

    override suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo> {
        if (browser != BrowserType.CHROME) return emptyList()

        val home = System.getProperty("user.home")
        val prefPath = "$home/Library/Application Support/Google/Chrome/Default/Preferences"
        val file = File(prefPath)

        return if (file.exists()) {
             try {
                chromeParser.parse(file.readText(), browser).extensions
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo? {
        return null
    }
}
