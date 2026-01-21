package info.benjaminhill.desktopguardian.platform

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FileLogger {
    private var isInitialized = false
    private lateinit var logFile: File

    fun setup() {
        if (isInitialized) return

        try {
            val userHome = System.getProperty("user.home")
            val logDir = File(userHome, ".desktopguardian/logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // yyyy-MM-dd
            val pid = ProcessHandle.current().pid()
            logFile = File(logDir, "desktopguardian-$dateStr-$pid.log")

            // Append mode
            val fileOut = FileOutputStream(logFile, true)

            val multiOut = MultiOutputStream(System.out, fileOut)
            val multiErr = MultiOutputStream(System.err, fileOut)

            System.setOut(PrintStream(multiOut))
            System.setErr(PrintStream(multiErr))

            isInitialized = true

            println("----------------------------------------------------------------")
            println("Log initialized at ${java.time.LocalDateTime.now()}")
            println("Log file: ${logFile.absolutePath}")
        } catch (e: Exception) {
            // Fallback to original stderr if logging fails
            System.err.println("Failed to initialize FileLogger: ${e.message}")
            e.printStackTrace()
        }
    }
}

private class MultiOutputStream(
    private val original: OutputStream,
    private val fileOut: OutputStream,
) : OutputStream() {
    override fun write(b: Int) {
        original.write(b)
        fileOut.write(b)
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        original.write(b, off, len)
        fileOut.write(b, off, len)
    }

    override fun flush() {
        original.flush()
        fileOut.flush()
    }

    override fun close() {
        original.close()
        fileOut.close()
    }
}
