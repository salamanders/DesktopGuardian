package info.benjaminhill.desktopguardian.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.Test
import kotlin.test.assertEquals

class DatabaseTest {

    @Test
    fun testDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        info.benjaminhill.desktopguardian.Schema.create(driver)
        val database = desktopguardian(driver)

        database.mainQueries.insertApp("App1", 123L, "1.0")
        val apps = database.mainQueries.selectAllApps().executeAsList()

        assertEquals(1, apps.size)
        assertEquals("App1", apps[0].name)
    }
}
