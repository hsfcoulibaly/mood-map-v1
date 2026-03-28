package hacklanta.moodmap.data.model

import com.google.gson.annotations.SerializedName

data class MoodPin(
    val id: String,
    val lat: Double,
    val lng: Double,
    val mood: String,
    val colorHex: String,
    val emoji: String,
    val timeLabel: String,
    val timestamp: Long,
    val isUserPin: Boolean = false,
)

data class SeedPinJson(
    val id: Int,
    val lat: Double,
    val lng: Double,
    val mood: String,
    val color: String,
    val emoji: String,
    val time: String,
) {
    fun toMoodPin(baseTime: Long, indexFromEnd: Int): MoodPin {
        val ts = baseTime - indexFromEnd * 3 * 60_000L
        return MoodPin(
            id = "seed_$id",
            lat = lat,
            lng = lng,
            mood = mood,
            colorHex = color,
            emoji = emoji,
            timeLabel = time,
            timestamp = ts,
            isUserPin = false,
        )
    }
}

data class MoodDefinition(
    val label: String,
    val emoji: String,
    val colorHex: String,
)

object MoodCatalog {
    val moods: List<MoodDefinition> = listOf(
        MoodDefinition("Happy", "😊", "#4CAF50"),
        MoodDefinition("Excited", "🤩", "#FF9800"),
        MoodDefinition("Anxious", "😰", "#9C27B0"),
        MoodDefinition("Stressed", "😤", "#F44336"),
        MoodDefinition("Sad", "😔", "#2196F3"),
    )

    val positiveMoods: Set<String> = setOf("Happy", "Excited")

    fun definitionFor(moodLabel: String): MoodDefinition =
        moods.find { it.label == moodLabel } ?: moods.first()
}

data class CampusInsights(
    val hotspot: String,
    val dominant: String,
    val alert: String,
    val vibe: String,
)

data class ComfortPayload(
    val message: String,
    val action: String,
    val joke: String,
    val reminder: String,
    val musicVibes: String? = null,
    val recoveryPrompt: String? = null,
)

data class JournalPinEntry(
    val id: String,
    val timeLabel: String,
    val mood: String,
    val emoji: String,
    val colorHex: String,
    val area: String,
)
