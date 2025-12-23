package info.benjaminhill.desktopguardian.platform

import java.io.File

object StartupManager {
    fun enableStartup() {
        val os = System.getProperty("os.name").lowercase()
        try {
            when {
                os.contains("win") -> enableWindowsStartup()
                os.contains("mac") -> enableMacOsStartup()
                else -> println("Startup persistence not supported on this OS")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disableStartup() {
        val os = System.getProperty("os.name").lowercase()
        try {
            when {
                os.contains("win") -> disableWindowsStartup()
                os.contains("mac") -> disableMacOsStartup()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enableWindowsStartup() {
        val startupDir = File(System.getenv("APPDATA"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
        if (startupDir.exists()) {
            val batFile = File(startupDir, "DesktopGuardian.bat")
            // Best-effort detection for development vs production
            val appPath = System.getProperty("java.class.path")

            // In a real packaged app (jpackage), we might need a fixed path or more robust detection.
            // For now, we use the classpath approach which works for 'java -jar' scenarios,
            // but we warn that it might not work for all distribution methods.
            // If running via Gradle, this might be a long classpath string which won't work in a BAT file directly.
            // A safer default for dev is just a log or a comment.

            val runCommand = if (appPath.contains("gradle")) {
               "echo 'Running from Gradle, skipping persistence setup'"
            } else {
               "@echo off\njavaw -jar \"$appPath\""
            }

            batFile.writeText(runCommand)
        }
    }

    private fun disableWindowsStartup() {
        val startupDir = File(System.getenv("APPDATA"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
        val batFile = File(startupDir, "DesktopGuardian.bat")
        if (batFile.exists()) batFile.delete()
    }

    private fun enableMacOsStartup() {
        val launchAgentsDir = File(System.getProperty("user.home"), "Library/LaunchAgents")
        if (!launchAgentsDir.exists()) launchAgentsDir.mkdirs()

        val plistFile = File(launchAgentsDir, "info.benjaminhill.desktopguardian.plist")
        val appPath = System.getProperty("java.class.path")

        // Similar to Windows, check if we are in a sane environment
        if (appPath.contains("gradle")) {
            println("Running from Gradle, skipping persistence setup")
            return
        }

        val plistContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>info.benjaminhill.desktopguardian</string>
                <key>ProgramArguments</key>
                <array>
                    <string>/usr/bin/java</string>
                    <string>-jar</string>
                    <string>$appPath</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
            </dict>
            </plist>
        """.trimIndent()

        plistFile.writeText(plistContent)
    }

    private fun disableMacOsStartup() {
        val launchAgentsDir = File(System.getProperty("user.home"), "Library/LaunchAgents")
        val plistFile = File(launchAgentsDir, "info.benjaminhill.desktopguardian.plist")
        if (plistFile.exists()) plistFile.delete()
    }
}
