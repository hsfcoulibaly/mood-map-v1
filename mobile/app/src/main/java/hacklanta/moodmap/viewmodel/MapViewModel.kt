package hacklanta.moodmap.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hacklanta.moodmap.R
import hacklanta.moodmap.data.local.MoodMapLocalStore
import hacklanta.moodmap.data.model.MoodDefinition
import hacklanta.moodmap.data.model.MoodPin
import hacklanta.moodmap.data.model.SeedPinJson
import hacklanta.moodmap.util.CampusUtils

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MoodMapLocalStore.get(application)

    private val _pins = mutableStateListOf<MoodPin>()
    val pins: List<MoodPin> get() = _pins

    var selectedPin by mutableStateOf<MoodPin?>(null)
        private set

    var showMoodPickerForLatLng by mutableStateOf<Pair<Double, Double>?>(null)
        private set

    var userPinCountToday by mutableIntStateOf(0)
        private set

    init {
        loadSeeds()
        loadUserPins()
        userPinCountToday = store.userPinsToday().size
    }

    private fun loadSeeds() {
        val ctx = getApplication<Application>()
        val json = ctx.resources.openRawResource(R.raw.seed_pins).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<SeedPinJson>>() {}.type
        val seeds: List<SeedPinJson> = Gson().fromJson(json, type) ?: emptyList()
        val base = System.currentTimeMillis()
        val n = seeds.size
        seeds.forEachIndexed { i, s ->
            _pins.add(s.toMoodPin(base, n - 1 - i))
        }
    }

    private fun loadUserPins() {
        for (p in store.userPinsToday()) {
            _pins.add(p)
        }
    }

    fun openCompanion(pin: MoodPin) {
        selectedPin = pin
    }

    fun dismissCompanion() {
        selectedPin = null
    }

    fun requestDropPin(lat: Double, lng: Double) {
        showMoodPickerForLatLng = lat to lng
    }

    fun cancelMoodPicker() {
        showMoodPickerForLatLng = null
    }

    fun confirmMoodForPendingPin(def: MoodDefinition) {
        val loc = showMoodPickerForLatLng ?: return
        showMoodPickerForLatLng = null
        val now = System.currentTimeMillis()
        val id = "local_$now"
        val timeLabel = CampusUtils.formatTimeLabel()
        val pin = MoodPin(
            id = id,
            lat = loc.first,
            lng = loc.second,
            mood = def.label,
            colorHex = def.colorHex,
            emoji = def.emoji,
            timeLabel = timeLabel,
            timestamp = now,
            isUserPin = true,
        )
        _pins.add(pin)
        store.bumpStreakIfNeeded()
        val userPins = store.userPinsToday() + pin
        store.saveUserPinsToday(userPins)
        store.setJournalSummary(null)
        userPinCountToday = userPins.size
    }

    fun recentPinCount(withinMs: Long = 5 * 60 * 1000L): Int {
        val cutoff = System.currentTimeMillis() - withinMs
        return _pins.count { it.timestamp >= cutoff }
    }

    /** Approximate “counselor alert” when many stressed pins cluster (web-style). */
    fun stressedClusterNearLibrary(): Boolean {
        val stressed = _pins.filter { it.mood == "Stressed" && it.lat in 33.748..33.752 }
        return stressed.size >= 4
    }
}
