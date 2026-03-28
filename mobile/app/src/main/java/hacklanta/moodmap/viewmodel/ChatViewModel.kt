package hacklanta.moodmap.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// A simple data class to represent a single message
data class ChatMessage(val text: String, val isFromUser: Boolean)

class ChatViewModel : ViewModel() {
    // This holds the list of messages and updates the UI automatically when changed
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    init {
        // A welcome message from Gemini when the screen loads
        _messages.add(ChatMessage("Hello! I'm your Mood Map guide. How are you feeling today?", isFromUser = false))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add the user's message to the chat
        _messages.add(ChatMessage(text, isFromUser = true))

        // 2. Simulate Gemini thinking and replying (We will replace this with the real API later)
        viewModelScope.launch {
            delay(1000) // Fake 1-second network delay
            _messages.add(ChatMessage("That is very interesting. Tell me more about why you feel that way.", isFromUser = false))
        }
    }
}