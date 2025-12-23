package info.benjaminhill.desktopguardian

import info.benjaminhill.desktopguardian.db.BrowserExtension
import info.benjaminhill.desktopguardian.db.InstalledApp
import info.benjaminhill.desktopguardian.db.SearchConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffEngineTest {

    private val diffEngine = DiffEngine { 1234567890L }

    @Test
    fun testDiffApps() {
        val currentApps = listOf(
            AppInfo("App1", "1.0", 0L),
            AppInfo("App2", "2.0", 0L)
        )
        val savedApps = listOf(
            InstalledApp(1, "App1", 0L, "1.0"),
            InstalledApp(2, "App3", 0L, "1.0")
        )

        val alerts = diffEngine.diffApps(currentApps, savedApps)

        assertEquals(2, alerts.size)
        assertTrue(alerts.any { it.type == AlertType.APP_ADDED && it.message.contains("App2") })
        assertTrue(alerts.any { it.type == AlertType.APP_REMOVED && it.message.contains("App3") })
    }

    @Test
    fun testDiffExtensions() {
        val currentExts = listOf(
            ExtensionInfo("id1", "Ext1", BrowserType.CHROME),
            ExtensionInfo("id2", "Ext2", BrowserType.CHROME)
        )
        val savedExts = listOf(
            BrowserExtension(1, "CHROME", "id1", "Ext1"),
            BrowserExtension(2, "CHROME", "id3", "Ext3")
        )

        val alerts = diffEngine.diffExtensions(currentExts, savedExts)

        assertEquals(2, alerts.size)
        assertTrue(alerts.any { it.type == AlertType.EXTENSION_ADDED && it.message.contains("Ext2") })
        assertTrue(alerts.any { it.type == AlertType.EXTENSION_REMOVED && it.message.contains("Ext3") })
    }

    @Test
    fun testDiffSearch() {
        val currentSearch = listOf(
            SearchProviderInfo(BrowserType.CHROME, "https://google.com")
        )
        val savedSearch = listOf(
            SearchConfig("CHROME", "https://yahoo.com")
        )

        val alerts = diffEngine.diffSearch(currentSearch, savedSearch)

        assertEquals(1, alerts.size)
        assertTrue(alerts.any { it.type == AlertType.SEARCH_CHANGED && it.details.contains("google.com") })
    }
}
