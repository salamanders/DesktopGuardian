package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.SystemMonitor

object SystemMonitorFactory {
    fun getSystemMonitor(): SystemMonitor {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> WindowsSystemMonitor()
            os.contains("mac") -> MacOsSystemMonitor()
            else -> LinuxSystemMonitor() // Fallback/Dev
        }
    }
}
