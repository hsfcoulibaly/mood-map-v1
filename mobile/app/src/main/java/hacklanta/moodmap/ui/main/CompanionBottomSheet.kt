package hacklanta.moodmap.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hacklanta.moodmap.ai.AiMoodService
import hacklanta.moodmap.data.model.ComfortPayload
import hacklanta.moodmap.data.model.MoodPin
import hacklanta.moodmap.util.CampusUtils
import hacklanta.moodmap.util.EmergencyDetector
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class CompanionChatLine(val text: String, val fromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionBottomSheet(
    pin: MoodPin?,
    onDismiss: () -> Unit,
) {
    if (pin == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var comfort by remember(pin.id) { mutableStateOf<ComfortPayload?>(null) }
    var loadingComfort by remember(pin.id) { mutableStateOf(true) }
    var comfortError by remember(pin.id) { mutableStateOf(false) }
    val chatLines = remember(pin.id) { mutableStateListOf<CompanionChatLine>() }
    var input by remember { mutableStateOf("") }
    var chatBusy by remember { mutableStateOf(false) }
    var emergencyLevel by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pin.id) {
        loadingComfort = true
        val seed = Random.nextInt(0, 10000)
        val result = AiMoodService.generateComfort(
            mood = pin.mood,
            timeOfDay = CampusUtils.getTimeOfDay(),
            pinNumber = 1,
            randomSeed = seed,
        )
        comfort = result.getOrNull()
        if (comfort == null) {
            comfortError = true
            comfort = AiMoodService.fallbackComfort()
        }
        loadingComfort = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "${pin.emoji} ${pin.mood} · ${pin.timeLabel}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                CampusUtils.getArea(pin.lat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (loadingComfort) {
                CircularProgressIndicator(Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
            } else {
                comfort?.let { c ->
                    ComfortCards(c, comfortError)
                }
            }

            Text(
                "Chat with companion",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (line in chatLines) {
                    Text(
                        text = (if (line.fromUser) "You: " else "Companion: ") + line.text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message…") },
                    singleLine = true,
                )
                IconButton(
                    onClick = {
                        val msg = input.trim()
                        if (msg.isEmpty() || chatBusy) return@IconButton
                        val level = EmergencyDetector.detectLevel(msg)
                        emergencyLevel = level
                        if (level >= 2) return@IconButton
                        chatLines.add(CompanionChatLine(msg, true))
                        input = ""
                        chatBusy = true
                        scope.launch {
                            val r = AiMoodService.chatWithMood(pin.mood, msg)
                            chatLines.add(
                                CompanionChatLine(
                                    r.getOrElse { e -> "Sorry: ${e.message}" },
                                    false,
                                ),
                            )
                            chatBusy = false
                        }
                    },
                    enabled = !chatBusy,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }

    if (emergencyLevel >= 2) {
        Dialog(
            onDismissRequest = { emergencyLevel = 0 },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (emergencyLevel >= 3) "Emergency support" else "Safety check-in",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        if (emergencyLevel >= 3) {
                            "If you are in immediate danger, call 911 or Campus Police. You can also call or text 988 (Suicide & Crisis Lifeline)."
                        } else {
                            "If you feel threatened or unsafe, move to a public place and contact Campus Police or someone you trust."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { emergencyLevel = 0 }) { Text("I understand") }
                }
            }
        }
    }
}

@Composable
private fun ComfortCards(c: ComfortPayload, usedFallback: Boolean) {
    if (usedFallback) {
        Text(
            "Offline-style comfort (add GEMINI_API_KEY for full AI).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    Card(Modifier.padding(vertical = 4.dp), colors = CardDefaults.cardColors()) {
        Text(c.message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
    Card(Modifier.padding(vertical = 4.dp)) {
        Text("Try this: ${c.action}", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
    Card(Modifier.padding(vertical = 4.dp)) {
        Text(c.joke, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
    Card(Modifier.padding(vertical = 4.dp)) {
        Text(c.reminder, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
    c.musicVibes?.takeIf { it.isNotBlank() }?.let { m ->
        Card(Modifier.padding(vertical = 4.dp)) {
            Text("Music: $m", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    c.recoveryPrompt?.takeIf { it.isNotBlank() }?.let { r ->
        Card(Modifier.padding(vertical = 4.dp)) {
            Text("Reflect: $r", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
