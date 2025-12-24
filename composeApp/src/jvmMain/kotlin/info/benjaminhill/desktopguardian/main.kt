package info.benjaminhill.desktopguardian

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    if (args.contains("--scan-only")) {
        runHeadlessScan()
    } else {
        runGui()
    }
}

fun runHeadlessScan() = runBlocking {
    println("Starting Headless Scan...")
    val manager = GuardianManager()

    // Print status updates to console
    val loggingJob = manager.status.onEach { status ->
        println("Status: $status")
    }.launchIn(this)

    manager.runScan()
    loggingJob.cancel()
    println("Scan Complete. Exiting.")
    // Application exits here, releasing all resources.
}

fun runGui() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DesktopGuardian",
    ) {
        App()
    }
}
