package info.benjaminhill.desktopguardian.engine

import info.benjaminhill.desktopguardian.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffEngineTest {

    private val fixedTime = 123456789L
    private val diffEngine = DiffEngine { fixedTime }

    @Test
    fun testDiffApps() {
        val oldApps = listOf(
            AppInfo("App1", "1.0", 100L),
            AppInfo("App2", "1.0", 100L)
        )
        val newApps = listOf(
            AppInfo("App1", "1.0", 100L),
            AppInfo("App3", "1.0", 100L) // Added
        )
        // App2 removed

        val alerts = diffEngine.diffApps(oldApps, newApps)
        assertEquals(2, alerts.size)

        val added = alerts.find { it.type == AlertType.APP_ADDED }
        assertEquals("App3", added?.details?.substringAfter("New App installed: ")?.substringBefore(" ("))
        assertEquals(fixedTime, added?.timestamp)

        val removed = alerts.find { it.type == AlertType.APP_REMOVED }
        assertEquals("App2", removed?.details?.substringAfter("App removed: "))
    }

    @Test
    fun testDiffExtensions() {
         val oldExts = listOf(
             ExtensionInfo("ext1", "AdBlock", "1.0", BrowserType.CHROME)
         )
         val newExts = listOf(
             ExtensionInfo("ext1", "AdBlock", "1.0", BrowserType.CHROME),
             ExtensionInfo("ext2", "Malware", "1.0", BrowserType.CHROME)
         )

         val alerts = diffEngine.diffExtensions(oldExts, newExts)
         assertEquals(1, alerts.size)
         assertEquals(AlertType.EXTENSION_ADDED, alerts[0].type)
    }

    @Test
    fun testDiffSearch() {
        val oldSearch = SearchProviderInfo("https://google.com", "Google", BrowserType.CHROME)
        val newSearch = SearchProviderInfo("https://evil.com", "Evil", BrowserType.CHROME)

        val alerts = diffEngine.diffSearch(oldSearch, newSearch)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.SEARCH_CHANGED, alerts[0].type)
        assertEquals(AlertSeverity.CRITICAL, alerts[0].severity)
    }

    @Test
    fun testDiffSearchNoChange() {
        val oldSearch = SearchProviderInfo("https://google.com", "Google", BrowserType.CHROME)
        val newSearch = SearchProviderInfo("https://google.com", "Google", BrowserType.CHROME)

        val alerts = diffEngine.diffSearch(oldSearch, newSearch)
        assertEquals(0, alerts.size)
    }
}
