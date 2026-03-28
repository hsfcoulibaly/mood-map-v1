package hacklanta.moodmap.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hacklanta.moodmap.data.local.MoodMapLocalStore

private val storyPlaceholders = listOf(
    "I went for a walk outside...",
    "I called my mom...",
    "I listened to my favourite song...",
    "I took a 10 min break and stretched...",
    "I got a coffee and watched the world go by...",
    "I talked to a friend about it...",
)

@Composable
fun RecoveryTabScreen() {
    val context = LocalContext.current
    val store = remember { MoodMapLocalStore.get(context) }
    val stories = remember { mutableStateListOf<String>().apply { addAll(store.loadRecoveryStories()) } }
    var draft by rememberSaveable { mutableStateOf("") }
    val placeholderHint = remember { storyPlaceholders.random() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Recovery stories", style = MaterialTheme.typography.titleLarge)
        Text(
            "Anonymous things that helped (like the web Recovery feed).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { v -> draft = v },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            minLines = 3,
            placeholder = { Text(placeholderHint) },
            label = { Text("What helped you bounce back?") },
        )
        Button(
            onClick = {
                val t = draft.trim()
                if (t.isNotEmpty()) {
                    stories.add(0, t)
                    draft = ""
                    store.saveRecoveryStories(stories.toList())
                }
            },
            modifier = Modifier.padding(vertical = 8.dp),
        ) { Text("Share anonymously") }

        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            items(stories.size) { index ->
                val s = stories[index]
                Text("• $s", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
