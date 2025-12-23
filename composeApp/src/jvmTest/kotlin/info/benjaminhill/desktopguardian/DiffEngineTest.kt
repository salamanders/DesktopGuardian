package info.benjaminhill.desktopguardian

import info.benjaminhill.desktopguardian.db.InstalledApp
import info.benjaminhill.desktopguardian.db.BrowserExtension
import info.benjaminhill.desktopguardian.db.SearchConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffEngineTest {

    private val diffEngine = DiffEngine { 1234567890L }

    @Test
    fun testDiffApps_Added() {
        val current = listOf(AppInfo("NewApp", "1.0", 1000L))
        val saved = emptyList<InstalledApp>()

        val alerts = diffEngine.diffApps(current, saved)

        assertEquals(1, alerts.size)
        assertEquals(AlertType.APP_ADDED, alerts[0].type)
        assertEquals("New app installed: NewApp (1.0)", alerts[0].details)
    }

    @Test
    fun testDiffApps_Removed() {
        val current = emptyList<AppInfo>()
        val saved = listOf(InstalledApp(1, "OldApp", 1000L, "1.0"))

        val alerts = diffEngine.diffApps(current, saved)

        assertEquals(1, alerts.size)
        assertEquals(AlertType.APP_REMOVED, alerts[0].type)
        assertTrue(alerts[0].details.contains("OldApp"))
    }

    @Test
    fun testDiffApps_Updated() {
        val current = listOf(AppInfo("App", "2.0", 2000L))
        val saved = listOf(InstalledApp(1, "App", 1000L, "1.0"))

        val alerts = diffEngine.diffApps(current, saved)

        assertEquals(1, alerts.size)
        assertEquals(AlertType.APP_UPDATED, alerts[0].type)
        assertTrue(alerts[0].details.contains("from 1.0 to 2.0"))
    }

    @Test
    fun testDiffExtensions_Added() {
        val current = listOf(ExtensionInfo("ext1", "AdBlock", BrowserType.CHROME))
        val saved = emptyList<BrowserExtension>()

        val alerts = diffEngine.diffExtensions(current, saved)

        assertEquals(1, alerts.size)
        assertEquals(AlertType.EXTENSION_ADDED, alerts[0].type)
        assertTrue(alerts[0].details.contains("AdBlock"))
    }

    @Test
    fun testDiffExtensions_Removed() {
        val current = emptyList<ExtensionInfo>()
        val saved = listOf(BrowserExtension(1, "CHROME", "ext1", "AdBlock"))

        val alerts = diffEngine.diffExtensions(current, saved)

        assertEquals(1, alerts.size)
        assertEquals(AlertType.EXTENSION_REMOVED, alerts[0].type)
        assertTrue(alerts[0].details.contains("AdBlock"))
    }

    @Test
    fun testDiffSearch_Changed() {
        val current = listOf(SearchProviderInfo(BrowserType.CHROME, "https://google.com"))
        val saved = listOf(SearchConfig("CHROME", "https://bing.com"))

        val alerts = diffEngine.diffSearch(current, saved)

        assertEquals(1, alerts.size)
        assertEquals(AlertType.SEARCH_CHANGED, alerts[0].type)
        assertTrue(alerts[0].details.contains("google.com"))
        assertTrue(alerts[0].details.contains("bing.com"))
    }

    @Test
    fun testDiffSearch_NoChange() {
        val current = listOf(SearchProviderInfo(BrowserType.CHROME, "https://google.com"))
        val saved = listOf(SearchConfig("CHROME", "https://google.com"))

        val alerts = diffEngine.diffSearch(current, saved)

        assertEquals(0, alerts.size)
    }
}
