package hacklanta.moodmap.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hacklanta.moodmap.ai.AiMoodService
import hacklanta.moodmap.data.model.CampusInsights
import hacklanta.moodmap.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@Composable
fun InsightsTabScreen(mapVm: MapViewModel) {
    var loading by remember { mutableStateOf(false) }
    var insights by remember { mutableStateOf<CampusInsights?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Campus insights", style = MaterialTheme.typography.titleLarge)
        Text(
            "Counselor-style read on anonymous map pins (same idea as the web /api/insights).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Button(
            onClick = {
                loading = true
                error = null
                scope.launch {
                    val r = AiMoodService.generateInsights(mapVm.pins)
                    insights = r.getOrNull()
                    error = r.exceptionOrNull()?.message
                    loading = false
                }
            },
            enabled = !loading && mapVm.pins.isNotEmpty(),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Refresh AI insights")
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        insights?.let { i ->
            Text("Hotspot", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            Text(i.hotspot)
            Text("Dominant mood", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text(i.dominant)
            Text("Counselor alert", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text(i.alert)
            Text("Campus vibe", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text(i.vibe)
        }
    }
}
