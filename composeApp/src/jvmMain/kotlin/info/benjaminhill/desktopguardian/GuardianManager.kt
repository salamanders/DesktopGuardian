package info.benjaminhill.desktopguardian

import info.benjaminhill.desktopguardian.alert.WebHookAlertService
import info.benjaminhill.desktopguardian.db.DatabaseDriverFactory
import info.benjaminhill.desktopguardian.db.desktopguardian
import info.benjaminhill.desktopguardian.platform.SystemMonitorFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates the scanning, diffing, and alerting process.
 * Acts as the bridge between:
 * - SystemMonitor (Raw OS Data)
 * - DiffEngine (Logic)
 * - Database (Persistence)
 * - WebHookAlertService (Side Effects)
 */
class GuardianManager {

    private val _status = MutableStateFlow("Initializing...")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _lastScan = MutableStateFlow<String>("Never")
    val lastScan: StateFlow<String> = _lastScan.asStateFlow()

    private val _alertEndpoint = MutableStateFlow("https://example.com/api/alert")
    val alertEndpoint: StateFlow<String> = _alertEndpoint.asStateFlow()

    private val monitor = SystemMonitorFactory.getSystemMonitor()
    private val webhookService = WebHookAlertService()

    // Database Initialization
    private val driver = DatabaseDriverFactory().createDriver()
    private val database = desktopguardian(driver)

    init {
        val savedEndpoint = database.mainQueries.selectConfig("alert_endpoint").executeAsOneOrNull()
        if (savedEndpoint != null) {
            _alertEndpoint.value = savedEndpoint
        }
    }

    fun updateAlertEndpoint(url: String) {
        _alertEndpoint.value = url
        database.mainQueries.insertConfig("alert_endpoint", url)
    }

    // DiffEngine is pure logic, separated from state
    private val diffEngine = DiffEngine { System.currentTimeMillis() }

    suspend fun runScan() {
        _status.value = "Scanning..."
        try {
            // 1. Get Current State (Raw OS Data)
            val currentApps = monitor.getInstalledApps()
            val currentExtensions = BrowserType.entries.flatMap { browser ->
                monitor.getBrowserExtensions(browser)
            }
            val currentSearch = BrowserType.entries.mapNotNull { browser ->
                monitor.getDefaultSearch(browser)
            }

            // 2. Get Saved State (Persistence)
            val savedApps = database.mainQueries.selectAllApps().executeAsList()
            val savedExtensions = database.mainQueries.selectAllExtensions().executeAsList()
            val savedSearch = database.mainQueries.selectAllSearchConfigs().executeAsList()

            // 3. Diff (Logic)
            val appAlerts = diffEngine.diffApps(currentApps, savedApps)
            val extAlerts = diffEngine.diffExtensions(currentExtensions, savedExtensions)
            val searchAlerts = diffEngine.diffSearch(currentSearch, savedSearch)

            val alerts = appAlerts + extAlerts + searchAlerts

            // 4. Alert (Side Effects)
            if (alerts.isNotEmpty()) {
                _status.value = "Changes Detected! Sending ${alerts.size} alerts..."
                alerts.forEach { alert ->
                    webhookService.sendAlert(alert, _alertEndpoint.value)
                }
            } else {
                _status.value = "System Healthy. No changes."
            }

            // 5. Update Persistence (Snapshot)
            database.transaction {
                database.mainQueries.deleteAllApps()
                currentApps.forEach { app ->
                    database.mainQueries.insertApp(app.name, app.installDate, app.version)
                }

                database.mainQueries.deleteAllExtensions()
                currentExtensions.forEach { ext ->
                    database.mainQueries.insertExtension(ext.browser.name, ext.id, ext.name)
                }

                database.mainQueries.deleteAllSearchConfigs()
                currentSearch.forEach { search ->
                    database.mainQueries.insertSearchConfig(search.browser.name, search.url)
                }
            }

            _lastScan.value = java.time.LocalDateTime.now().toString()

        } catch (e: Exception) {
            _status.value = "Error: ${e.message}"
            e.printStackTrace()
        }
    }
}
