package info.benjaminhill.desktopguardian.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {

    @Test
    fun testDatabaseCreationAndInsert() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        desktopguardian.Schema.create(driver)
        val database = desktopguardian(driver)

        // Insert an app
        database.mainQueries.insertApp(
            name = "Test App",
            installDate = 123456789L,
            version = "1.0.0"
        )

        // Select it back
        val apps = database.mainQueries.selectAllApps().executeAsList()
        assertEquals(1, apps.size)
        assertEquals("Test App", apps[0].name)
        assertEquals(123456789L, apps[0].installDate)
        assertEquals("1.0.0", apps[0].version)

        // Insert extension
        database.mainQueries.insertExtension(
            browser = "Chrome",
            extensionId = "xyz",
            name = "AdBlock"
        )
        val extensions = database.mainQueries.selectAllExtensions().executeAsList()
        assertEquals(1, extensions.size)
        assertEquals("AdBlock", extensions[0].name)
    }
}
