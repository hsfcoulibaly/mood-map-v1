package hacklanta.moodmap.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hacklanta.moodmap.data.model.JournalPinEntry
import hacklanta.moodmap.data.model.MoodPin
import hacklanta.moodmap.util.CampusUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MoodMapLocalStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun ensureDay() {
        val key = prefs.getString(KEY_JOURNAL_DATE, null)
        val today = todayStr()
        if (key != today) {
            prefs.edit()
                .putString(KEY_JOURNAL_DATE, today)
                .remove(KEY_USER_PINS)
                .remove(KEY_JOURNAL_SUMMARY)
                .apply()
        }
    }

    fun userPinsToday(): List<MoodPin> {
        ensureDay()
        val raw = prefs.getString(KEY_USER_PINS, null) ?: return emptyList()
        val type = object : TypeToken<List<MoodPin>>() {}.type
        return gson.fromJson(raw, type) ?: emptyList()
    }

    fun saveUserPinsToday(pins: List<MoodPin>) {
        ensureDay()
        prefs.edit().putString(KEY_USER_PINS, gson.toJson(pins)).apply()
    }

    fun journalEntriesFromPins(userPins: List<MoodPin>): List<JournalPinEntry> =
        userPins.map { pin ->
            JournalPinEntry(
                id = pin.id,
                timeLabel = pin.timeLabel,
                mood = pin.mood,
                emoji = pin.emoji,
                colorHex = pin.colorHex,
                area = CampusUtils.getArea(pin.lat),
            )
        }

    fun getStreakCount(): Int = prefs.getInt(KEY_STREAK_COUNT, 0)
    fun getStreakLast(): String = prefs.getString(KEY_STREAK_LAST, "") ?: ""

    /** Matches web bumpStreak: consecutive calendar days. */
    fun bumpStreakIfNeeded(): Pair<Int, String> {
        val today = todayStr()
        val last = getStreakLast()
        if (last == today) return getStreakCount() to last
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val newCount = if (last == yesterday) getStreakCount() + 1 else 1
        prefs.edit()
            .putInt(KEY_STREAK_COUNT, newCount)
            .putString(KEY_STREAK_LAST, today)
            .apply()
        return newCount to today
    }

    fun getJournalSummary(): String? = prefs.getString(KEY_JOURNAL_SUMMARY, null)

    fun setJournalSummary(text: String?) {
        if (text == null) prefs.edit().remove(KEY_JOURNAL_SUMMARY).apply()
        else prefs.edit().putString(KEY_JOURNAL_SUMMARY, text).apply()
    }

    fun loadRecoveryStories(): MutableList<String> {
        val raw = prefs.getString(KEY_RECOVERY, null) ?: return mutableListOf()
        val type = object : TypeToken<List<String>>() {}.type
        return (gson.fromJson<List<String>>(raw, type) ?: emptyList()).toMutableList()
    }

    fun saveRecoveryStories(stories: List<String>) {
        prefs.edit().putString(KEY_RECOVERY, gson.toJson(stories)).apply()
    }

    fun deviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    companion object {
        private const val PREFS = "moodmap_local"
        private const val KEY_JOURNAL_DATE = "moodmap_journal_date"
        private const val KEY_USER_PINS = "moodmap_user_pins"
        private const val KEY_JOURNAL_SUMMARY = "moodmap_journal_summary"
        private const val KEY_STREAK_COUNT = "moodmap_streak_count"
        private const val KEY_STREAK_LAST = "moodmap_streak_last"
        private const val KEY_RECOVERY = "moodmap_recovery_stories"
        private const val KEY_DEVICE_ID = "moodmap_device_id"

        fun get(context: Context): MoodMapLocalStore {
            return MoodMapLocalStore(context.applicationContext)
        }
    }
}
