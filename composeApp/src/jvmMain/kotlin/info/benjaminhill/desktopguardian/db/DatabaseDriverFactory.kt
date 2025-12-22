package info.benjaminhill.desktopguardian.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class DatabaseDriverFactory {
    fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home")
        val dbFile = File(userHome, "desktop_guardian.db")
        val dbExists = dbFile.exists() && dbFile.length() > 0

        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        val driver = JdbcSqliteDriver(url)

        if (!dbExists) {
            desktopguardian.Schema.create(driver)
        }

        return driver
    }
}
