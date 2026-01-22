package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import info.benjaminhill.desktopguardian.ExtensionInfo
import info.benjaminhill.desktopguardian.SearchProviderInfo
import info.benjaminhill.desktopguardian.SystemMonitor
import info.benjaminhill.desktopguardian.parsers.ChromePreferencesParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * Base implementation for SystemMonitor using osquery.
 * Platform-specific implementations can override query logic if needed,
 * or we can use conditional logic here.
 */
abstract class OsQuerySystemMonitor : SystemMonitor {
    protected val osQueryClient = OsQueryClient()
    protected val chromeParser = ChromePreferencesParser()
    protected val json = Json { ignoreUnknownKeys = true }

    // Data classes for OsQuery results
    @Serializable
    protected data class OsQueryApp(
        val name: String,
        val version: String? = null,
        val bundle_short_version: String? = null
    )

    @Serializable
    protected data class OsQueryExtension(
        val identifier: String,
        val name: String
    )

    override suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo> {
        if (!osQueryClient.isAvailable()) {
            println("osqueryi not found. Cannot fetch extensions.")
            return emptyList()
        }

        // osquery 'chrome_extensions' table covers Chrome.
        // Edge might be supported via 'ie_extensions' or similar on Windows,
        // but typically 'chrome_extensions' works for Chromium based browsers if path is right.
        // However, standard osquery 'chrome_extensions' usually just checks Chrome.
        // For now, we'll assume it covers what we need or return empty for non-Chrome if osquery is limited.

        if (browser != BrowserType.CHROME) {
            // TODO: Investigate osquery support for other browsers or configure paths
            return emptyList()
        }

        return try {
            val jsonOutput = osQueryClient.execute("SELECT identifier, name FROM chrome_extensions;")
            val rawList = json.parseToJsonElement(jsonOutput).jsonArray

            rawList.map { element ->
                val item = json.decodeFromJsonElement<OsQueryExtension>(element)
                ExtensionInfo(
                    id = item.identifier,
                    name = item.name,
                    browser = browser
                )
            }
        } catch (e: Exception) {
            println("Error fetching extensions from osquery: $e")
            emptyList()
        }
    }

    // Default implementation uses the manual parser (hybrid approach)
    override suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo? {
        val file = getPreferencesFile(browser) ?: return null
        return try {
            chromeParser.parse(file.readText(), browser).searchProvider
        } catch (_: Exception) {
            null
        }
    }

    protected abstract fun getPreferencesFile(browser: BrowserType): File?
}
