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

    private fun getExecutableCommand(): List<String> {
        val jpackagePath = System.getProperty("jpackage.app-path")
        if (jpackagePath != null) {
            return listOf(jpackagePath)
        }

        val appPath = System.getProperty("java.class.path")
        val javaHome = System.getProperty("java.home")

        // Basic dev/gradle detection
        if (appPath.contains("gradle")) {
             return emptyList()
        }

        // Fallback for non-jpackage execution (e.g. java -jar)
        // Note: On Windows 'javaw' is preferred for no-console
        val javaBin = if (System.getProperty("os.name").lowercase().contains("win")) "javaw" else "java"
        return listOf(javaBin, "-jar", appPath)
    }

    private fun enableWindowsStartup() {
        val startupDir = File(System.getenv("APPDATA"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
        if (startupDir.exists()) {
            val batFile = File(startupDir, "DesktopGuardian.bat")
            val commandParts = getExecutableCommand()

            if (commandParts.isEmpty()) {
               println("Running from Gradle, skipping persistence setup")
               return
            }

            // If jpackage (single element), we just run it.
            // If java -jar (multiple elements), we quote path.

            val runCommand = if (commandParts.size == 1) {
                "@echo off\n\"${commandParts[0]}\""
            } else {
                 // java -jar "path"
                 // commandParts[0] is java/javaw, commandParts[1] is -jar, commandParts[2] is path
                 "@echo off\n${commandParts[0]} ${commandParts[1]} \"${commandParts[2]}\""
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
        val commandParts = getExecutableCommand()

        if (commandParts.isEmpty()) {
            println("Running from Gradle, skipping persistence setup")
            return
        }

        val argsXml = commandParts.joinToString("\n") { "<string>$it</string>" }

        val plistContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>info.benjaminhill.desktopguardian</string>
                <key>ProgramArguments</key>
                <array>
                    $argsXml
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
