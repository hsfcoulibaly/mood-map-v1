package hacklanta.moodmap.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import hacklanta.moodmap.BuildConfig
import hacklanta.moodmap.data.model.CampusInsights
import hacklanta.moodmap.data.model.ComfortPayload
import hacklanta.moodmap.data.model.MoodCatalog
import hacklanta.moodmap.data.model.MoodPin
import hacklanta.moodmap.util.CampusUtils

object AiMoodService {

    private val gson = Gson()

    private fun model(): GenerativeModel? {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) return null
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key,
        )
    }

    fun stripCodeFence(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
            val end = t.lastIndexOf("```")
            if (end >= 0) t = t.substring(0, end).trim()
        }
        return t
    }

    suspend fun generateComfort(
        mood: String,
        timeOfDay: String,
        pinNumber: Int,
        randomSeed: Int,
    ): Result<ComfortPayload> {
        val m = model() ?: return Result.failure(IllegalStateException("no_api_key"))
        val isPositive = MoodCatalog.positiveMoods.contains(mood)
        val jsonShape = if (isPositive) {
            """{"message":"...","action":"...","joke":"...","reminder":"...","musicVibes":"...","recoveryPrompt":"..."}"""
        } else {
            """{"message":"...","action":"...","joke":"...","reminder":"...","musicVibes":"...","recoveryPrompt":"..."}"""
        }
        val prompt = """
            You are Mood Map's AI companion for college students (same role as the web app).
            Return ONLY valid JSON, no markdown, matching this shape: $jsonShape
            Student mood: $mood. Time of day: $timeOfDay. Check-in number today: $pinNumber. Variety seed: $randomSeed.
            Be warm and specific; avoid therapy clichés like "I hear you" or "It's okay to feel".
        """.trimIndent()
        return try {
            val raw = m.generateContent(prompt).text ?: return Result.failure(IllegalStateException("empty"))
            val json = stripCodeFence(raw)
            Result.success(gson.fromJson(json, ComfortPayload::class.java))
        } catch (e: JsonSyntaxException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateInsights(pins: List<MoodPin>): Result<CampusInsights> {
        val m = model() ?: return Result.failure(IllegalStateException("no_api_key"))
        if (pins.isEmpty()) return Result.failure(IllegalStateException("no_pins"))
        val summary = pins.joinToString("\n") { p ->
            "${p.mood} near ${CampusUtils.getArea(p.lat)}"
        }
        val prompt = """
            Anonymous campus mood map pins:
            $summary
            Return ONLY JSON: {"hotspot":"...","dominant":"...","alert":"...","vibe":"..."}
            hotspot = one sentence about the most intense emotional area; alert = one actionable counselor recommendation.
        """.trimIndent()
        return try {
            val raw = m.generateContent(prompt).text ?: return Result.failure(IllegalStateException("empty"))
            val json = stripCodeFence(raw)
            Result.success(gson.fromJson(json, CampusInsights::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarizeJournal(entries: List<String>): Result<String> {
        val m = model() ?: return Result.failure(IllegalStateException("no_api_key"))
        if (entries.isEmpty()) return Result.failure(IllegalStateException("empty"))
        val prompt = """
            Briefly reflect on these same-day student mood check-ins (2-4 sentences, supportive):
            ${entries.joinToString("\n") { "- $it" }}
        """.trimIndent()
        return try {
            val text = m.generateContent(prompt).text?.trim()
            if (text.isNullOrBlank()) Result.failure(IllegalStateException("empty"))
            else Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatWithMood(moodContext: String?, userMessage: String): Result<String> {
        val m = model() ?: return Result.failure(IllegalStateException("no_api_key"))
        val prompt = buildString {
            append("You are Mood Map's empathetic campus mental health assistant. Reply concisely.\n")
            append("Map / pin mood context: ${moodContext ?: "not specified"}.\n")
            append("User: ")
            append(userMessage)
        }
        return try {
            val reply = m.generateContent(prompt).text?.trim()
            if (reply.isNullOrBlank()) Result.failure(IllegalStateException("empty"))
            else Result.success(reply)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fallbackComfort(): ComfortPayload = ComfortPayload(
        message = "I'm here with you. Whatever you're feeling right now is valid.",
        action = "Take three slow, deep breaths.",
        joke = "Why do we tell actors to 'break a leg?' Because every play has a cast 😄",
        reminder = "You showed up today. That already takes courage.",
        musicVibes = null,
        recoveryPrompt = null,
    )
}
