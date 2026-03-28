package hacklanta.moodmap.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import hacklanta.moodmap.data.local.SessionStore

private val crisisActions = listOf(
    "Open emergency counseling walk-ins" to "Walk-in center notified — opening immediately",
    "Send campus-wide wellness notification" to "Notification queued for student devices",
    "Deploy peer support team to hotspot zones" to "Team dispatched — ETA 4 minutes",
)

@Composable
fun MenuTabScreen(navController: NavController) {
    var confirmed by remember { mutableStateOf(setOf<Int>()) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Crisis & safety", style = MaterialTheme.typography.titleLarge)
        Text(
            "Elevated stress flow from the web MoodMap demo. These are UI confirmations only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        crisisActions.forEachIndexed { i, pair ->
            val (label, confirmText) = pair
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (confirmed.contains(i)) {
                Text("✓ $confirmText", color = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = { confirmed = confirmed + i }) {
                    Text("Confirm action")
                }
            }
        }

        Text("Emergency numbers", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
        Text("988 — Suicide & Crisis Lifeline (call or text)")
        Text("911 — Emergency")
        Text("Campus police — add your campus number in settings.")

        Button(
            onClick = {
                SessionStore.clear()
                navController.navigate("welcome") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            modifier = Modifier.padding(top = 24.dp),
        ) { Text("Sign out") }
    }
}
