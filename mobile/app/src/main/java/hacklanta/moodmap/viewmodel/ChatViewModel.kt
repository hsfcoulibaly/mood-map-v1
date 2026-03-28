package hacklanta.moodmap.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import hacklanta.moodmap.BuildConfig
import hacklanta.moodmap.util.EmergencyDetector
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isFromUser: Boolean)

class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val chat = buildGeminiChat()

    init {
        _messages.add(ChatMessage("Hello! I'm your Mood Map guide. How are you feeling today?", isFromUser = false))
    }

    private fun checkEmergencyLevel(input: String): Int = EmergencyDetector.detectLevel(input)

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        _messages.add(ChatMessage(userText, isFromUser = true))

        val safetyLevel = checkEmergencyLevel(userText)

        viewModelScope.launch {
            try {
                if (safetyLevel >= 3) {
                    _messages.add(
                        ChatMessage(
                            "If you are in immediate danger, call 911. You can also call or text 988 (Suicide & Crisis Lifeline). You are not alone.",
                            isFromUser = false,
                        ),
                    )
                } else if (safetyLevel == 2) {
                    _messages.add(
                        ChatMessage(
                            "If you feel unsafe or threatened, go to a public place and contact Campus Police or someone you trust.",
                            isFromUser = false,
                        ),
                    )
                } else if (chat == null) {
                    _messages.add(
                        ChatMessage(
                            "Set GEMINI_API_KEY in mobile/local.properties to enable the AI assistant. If you're in crisis, contact 988 (Suicide & Crisis Lifeline) or local emergency services.",
                            isFromUser = false
                        )
                    )
                } else {
                    val response = chat.sendMessage(userText)
                    _messages.add(ChatMessage(response.text ?: "I'm here for you, but I'm having trouble connecting.", isFromUser = false))
                }
            } catch (e: Exception) {
                _messages.add(ChatMessage("Error: ${e.localizedMessage}", isFromUser = false))
            }
        }
    }

    private fun buildGeminiChat() = run {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) null
        else GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key
        ).startChat(
            history = listOf(
                content(role = "user") { text("You are a helpful, empathetic campus mental health assistant for an app called Mood Map. Keep your answers supportive and concise.") },
                content(role = "model") { text("Understood. I am here to support the students and provide a safe space for their feelings.") }
            )
        )
    }
}