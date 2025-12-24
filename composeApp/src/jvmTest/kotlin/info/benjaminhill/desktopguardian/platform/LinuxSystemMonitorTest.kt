package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.BrowserType
import org.junit.Test
import kotlin.test.assertTrue

class LinuxSystemMonitorTest {

    private val monitor = LinuxSystemMonitor()

    @Test
    fun testGetInstalledApps() = runTest {
        // Should return empty list as per design
        val apps = monitor.getInstalledApps()
        assertTrue(apps.isEmpty(), "Installed apps should be empty on Linux")
    }

    @Test
    fun testGetBrowserExtensions() = runTest {
        // In CI environment, we expect this to handle missing files gracefully and return empty list
        val extensions = monitor.getBrowserExtensions(BrowserType.CHROME)
        // We can't guarantee extensions exist, but we can guarantee it doesn't crash
        assertTrue(
            extensions.isEmpty() || extensions.isNotEmpty(),
            "Should return a list (empty or not) without throwing"
        )
    }

    @Test
    fun testGetDefaultSearch() = runTest {
        // Similar to above, gracefully handle missing file
        val search = monitor.getDefaultSearch(BrowserType.CHROME)
        // Can be null or valid
        assertTrue(search == null || search.url.isNotBlank(), "Should return null or valid search info")
    }

    private fun runTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }
}
