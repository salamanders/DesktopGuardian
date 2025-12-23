package info.benjaminhill.desktopguardian

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.benjaminhill.desktopguardian.platform.StartupManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val manager = remember { GuardianManager() }
    val scope = rememberCoroutineScope()
    val status by manager.status.collectAsState()
    val lastScan by manager.lastScan.collectAsState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Desktop Guardian",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Status: $status")
            Text("Last Scan: $lastScan")

            Spacer(modifier = Modifier.height(16.dp))

            val alertEndpoint by manager.alertEndpoint.collectAsState()
            OutlinedTextField(
                value = alertEndpoint,
                onValueChange = { manager.updateAlertEndpoint(it) },
                label = { Text("Alert API Endpoint") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                scope.launch {
                    manager.runScan()
                }
            }) {
                Text("Force Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { StartupManager.enableStartup() }) {
                    Text("Enable Startup")
                }

                Button(onClick = { StartupManager.disableStartup() }) {
                    Text("Disable Startup")
                }
            }
        }
    }
}
