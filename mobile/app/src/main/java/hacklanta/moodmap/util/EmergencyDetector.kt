package hacklanta.moodmap.util

/**
 * Port of [detectLevel](https://github.com/HassanCoulibaly/hacklanta-the-Cognitive-Coders/blob/main/src/utils.js)
 * from the MoodMap web app (L1 distress → L3 emergency).
 */
object EmergencyDetector {

    private val l1Words = listOf(
        "scared", "worried", "nervous", "anxious", "afraid",
        "uncomfortable", "unsafe feeling", "uneasy", "freaked out",
    )

    private val l2Words = listOf(
        "following me", "someone is watching", "feel threatened", "this person",
        "he wont leave", "he won't leave", "im being", "i'm being",
        "being watched", "someone following", "wont leave me alone",
        "won't leave me alone", "making me uncomfortable", "keeps following",
    )

    private val l3Words = listOf(
        "suicide", "kill myself", "hurt myself", "killing myself",
        "help me", "attack", "attacked", "he hit", "she hit", "im being attacked",
        "i'm being attacked", "call police", "call 911", "emergency", "911",
        "weapon", "knife", "gun", "bleeding", "i cant get away", "i can't get away",
        "he wont let me leave", "he won't let me leave", "going to hurt",
        "going to kill", "rape", "kidnap", "chasing me", "please help",
        "cant breathe", "can't breathe",
    )

    fun detectLevel(text: String): Int {
        val lower = text.lowercase().trim()
        if (l3Words.any { lower.contains(it) }) return 3
        if (l2Words.any { lower.contains(it) }) return 2
        if (l1Words.any { lower.contains(it) }) return 1
        return 0
    }
}
