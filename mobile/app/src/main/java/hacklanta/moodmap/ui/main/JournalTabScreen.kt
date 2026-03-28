package hacklanta.moodmap.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hacklanta.moodmap.ai.AiMoodService
import hacklanta.moodmap.data.local.MoodMapLocalStore
import hacklanta.moodmap.util.CampusUtils
import hacklanta.moodmap.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@Composable
fun JournalTabScreen(mapVm: MapViewModel) {
    val context = LocalContext.current
    val store = remember { MoodMapLocalStore.get(context) }
    val userPins = mapVm.pins.filter { it.isUserPin }
    var summary by remember { mutableStateOf(store.getJournalSummary()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userPins.size) {
        summary = store.getJournalSummary()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("My mood today", style = MaterialTheme.typography.titleLarge)
        val streak = store.getStreakCount()
        if (streak >= 2) {
            Text("🔥 $streak day streak", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        }

        if (userPins.isEmpty()) {
            Text(
                "Long-press the map to drop your first mood pin today.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
            ) {
                items(userPins, key = { it.id }) { pin ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val c = remember(pin.colorHex) {
                            runCatching { Color(android.graphics.Color.parseColor(pin.colorHex)) }
                                .getOrElse { Color(0xFF6750A4) }
                        }
                        Spacer(
                            Modifier
                                .size(12.dp)
                                .background(c, CircleShape),
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(pin.timeLabel, style = MaterialTheme.typography.labelSmall)
                            Text("${pin.emoji} ${pin.mood} near ${CampusUtils.getArea(pin.lat)}")
                        }
                    }
                }
            }

            if (userPins.size >= 2) {
                Button(
                    onClick = {
                        loading = true
                        scope.launch {
                            val lines = userPins.map { "${it.emoji} ${it.mood} near ${CampusUtils.getArea(it.lat)}" }
                            val r = AiMoodService.summarizeJournal(lines)
                            summary = r.getOrNull()
                            store.setJournalSummary(summary)
                            loading = false
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(20.dp).padding(end = 8.dp))
                        }
                        Text(if (loading) "Working…" else "Reflect on my day (AI)")
                    }
                }
                summary?.let { s ->
                    Text(s, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
