package hacklanta.moodmap.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CampusUtils {
    const val GSU_LAT = 33.7490
    const val GSU_LNG = -84.3880

    fun getTimeOfDay(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            h < 12 -> "morning"
            h < 17 -> "afternoon"
            h < 21 -> "evening"
            else -> "night"
        }
    }

    fun getArea(lat: Double): String {
        return when {
            lat in 33.750..33.752 -> "GSU Library"
            lat in 33.748..33.750 -> "Student Center"
            lat in 33.746..33.748 -> "Classroom South"
            else -> "Five Points"
        }
    }

    fun formatTimeLabel(): String =
        SimpleDateFormat("h:mm a", Locale.US).format(Date())
}
