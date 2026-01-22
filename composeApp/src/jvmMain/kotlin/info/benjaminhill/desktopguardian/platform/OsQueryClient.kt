package info.benjaminhill.desktopguardian.platform

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OsQueryClient {

    fun execute(query: String): String {
        // Run osqueryi in interactive mode with --json output
        val process = ProcessBuilder("osqueryi", "--json", query)
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                output.append(line)
                line = reader.readLine()
            }
        }

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("osqueryi timed out")
        }

        if (process.exitValue() != 0) {
            throw RuntimeException("osqueryi failed with exit code ${process.exitValue()}: $output")
        }

        return output.toString()
    }

    fun isAvailable(): Boolean {
        return try {
            ProcessBuilder("osqueryi", "--version")
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            false
        }
    }
}
