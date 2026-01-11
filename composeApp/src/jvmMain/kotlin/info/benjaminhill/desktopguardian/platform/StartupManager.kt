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
        // 1. Delete legacy startup script
        val startupDir = File(System.getenv("APPDATA"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
        val batFile = File(startupDir, "DesktopGuardian.bat")
        if (batFile.exists()) {
            try {
                batFile.delete()
                println("Legacy startup script deleted.")
            } catch (e: Exception) {
                println("Failed to delete legacy startup script: ${e.message}")
            }
        }

        // 2. Create Scheduled Task
        val commandParts = getExecutableCommand()
        if (commandParts.isEmpty()) {
            println("Running from Gradle, skipping persistence setup")
            return
        }

        // Construct command for schtasks /TR
        // For java -jar, we need: java -jar "path" --scan-only
        // For jpackage, we need: "path" --scan-only
        // schtasks /TR expects the command to be wrapped.
        // If the path has spaces, it needs inner quotes.

        val taskRunCommand = if (commandParts.size == 1) {
            // jpackage: "C:\Path\To\Exe" --scan-only
            "\"${commandParts[0]}\" --scan-only"
        } else {
            // java -jar: java -jar "C:\Path\To\Jar" --scan-only
            // Note: using javaw for java -jar might hide console, but --scan-only is headless anyway.
            // We use commandParts[0] (java/javaw), commandParts[1] (-jar), commandParts[2] (path)
            "${commandParts[0]} ${commandParts[1]} \"${commandParts[2]}\" --scan-only"
        }

        try {
            // Create XML file for advanced settings (StartWhenAvailable)
            val xmlContent = createWindowsTaskXml(taskRunCommand)
            val tempXml = File.createTempFile("desktopguardian_task", ".xml")
            tempXml.writeText(xmlContent)

            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "schtasks", "/Create",
                    "/XML", tempXml.absolutePath,
                    "/TN", "DesktopGuardianScan",
                    "/F" // Force overwrite
                )
            )

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("Windows Scheduled Task created successfully.")
            } else {
                val error = process.errorStream.bufferedReader().readText()
                println("Failed to create Windows Scheduled Task. Exit code: $exitCode. Error: $error")
            }
            tempXml.delete()
        } catch (e: Exception) {
            println("Error executing schtasks: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createWindowsTaskXml(command: String): String {
        val escapedCommand = command
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Task version="1.2" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task">
              <RegistrationInfo>
                <Description>Daily scan for Desktop Guardian.</Description>
              </RegistrationInfo>
              <Triggers>
                <CalendarTrigger>
                  <StartBoundary>2024-01-01T10:00:00</StartBoundary>
                  <Enabled>true</Enabled>
                  <ScheduleByDay>
                    <DaysInterval>1</DaysInterval>
                  </ScheduleByDay>
                </CalendarTrigger>
              </Triggers>
              <Principals>
                <Principal id="Author">
                  <LogonType>InteractiveToken</LogonType>
                  <RunLevel>LeastPrivilege</RunLevel>
                </Principal>
              </Principals>
              <Settings>
                <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>
                <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
                <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
                <AllowHardTerminate>true</AllowHardTerminate>
                <StartWhenAvailable>true</StartWhenAvailable>
                <RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>
                <IdleSettings>
                  <StopOnIdleEnd>true</StopOnIdleEnd>
                  <RestartOnIdle>false</RestartOnIdle>
                </IdleSettings>
                <AllowStartOnDemand>true</AllowStartOnDemand>
                <Enabled>true</Enabled>
                <Hidden>false</Hidden>
                <RunOnlyIfIdle>false</RunOnlyIfIdle>
                <WakeToRun>false</WakeToRun>
                <ExecutionTimeLimit>PT1H</ExecutionTimeLimit>
                <Priority>7</Priority>
              </Settings>
              <Actions Context="Author">
                <Exec>
                  <Command>cmd.exe</Command>
                  <Arguments>/c start "" /MIN $escapedCommand</Arguments>
                </Exec>
              </Actions>
            </Task>
        """.trimIndent()
    }

    private fun disableWindowsStartup() {
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "schtasks", "/Delete",
                    "/TN", "DesktopGuardianScan",
                    "/F"
                )
            )
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also ensure legacy file is gone
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

        // Add --scan-only to the arguments
        val allArgs = commandParts + "--scan-only"
        val argsXml = allArgs.joinToString("\n") { "<string>$it</string>" }

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
                <key>StartCalendarInterval</key>
                <dict>
                    <key>Hour</key>
                    <integer>10</integer>
                    <key>Minute</key>
                    <integer>0</integer>
                </dict>
                <key>StandardOutPath</key>
                <string>/tmp/desktopguardian.log</string>
                <key>StandardErrorPath</key>
                <string>/tmp/desktopguardian.error.log</string>
            </dict>
            </plist>
        """.trimIndent()

        plistFile.writeText(plistContent)
        println("macOS LaunchAgent created at ${plistFile.absolutePath}")
    }

    private fun disableMacOsStartup() {
        val launchAgentsDir = File(System.getProperty("user.home"), "Library/LaunchAgents")
        val plistFile = File(launchAgentsDir, "info.benjaminhill.desktopguardian.plist")
        if (plistFile.exists()) plistFile.delete()
    }
}
