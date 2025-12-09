package com.ishabdullah.aiish.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishabdullah.aiish.domain.models.Message
import com.ishabdullah.aiish.domain.models.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Chat ViewModel - Manages chat conversation state
 */
class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _shouldOpenCamera = MutableStateFlow(false)
    val shouldOpenCamera: StateFlow<Boolean> = _shouldOpenCamera.asStateFlow()

    fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                // Check for vision trigger
                if (isVisionQuery(content)) {
                    _shouldOpenCamera.value = true
                    return@launch
                }

                // Add user message
                val userMessage = Message(
                    content = content,
                    role = MessageRole.USER
                )
                _messages.value = _messages.value + userMessage

                _isLoading.value = true

                // Simulate AI response (replace with actual LLM call)
                val aiResponse = generateResponse(content)

                val assistantMessage = Message(
                    content = aiResponse,
                    role = MessageRole.ASSISTANT
                )
                _messages.value = _messages.value + assistantMessage

            } catch (e: Exception) {
                Timber.e(e, "Error sending message")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isVisionQuery(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return lowerMessage.contains("what do you see") ||
                lowerMessage.contains("describe this") ||
                lowerMessage.contains("look at") ||
                lowerMessage.contains("what's this") ||
                lowerMessage.contains("vision mode")
    }

    fun resetCameraTrigger() {
        _shouldOpenCamera.value = false
    }

    private suspend fun generateResponse(userMessage: String): String {
        // Placeholder response - will be replaced with actual LLM integration
        return when {
            userMessage.lowercase().contains("hello") || userMessage.lowercase().contains("hey") ->
                "Hello! I'm Ish, your private AI companion. How can I help you today?"

            userMessage.lowercase().contains("weather") ->
                "I can fetch real-time weather data for you using the OpenMeteo API. What location would you like weather for?"

            userMessage.lowercase().contains("price") || userMessage.lowercase().contains("bitcoin") ->
                "I can check cryptocurrency prices using CoinGecko. Which crypto would you like to know about?"

            userMessage.lowercase().contains("math") || userMessage.contains(Regex("\\d+\\s*[+\\-*/]\\s*\\d+")) ->
                "I have a deterministic math reasoner that can solve equations step-by-step. Let me calculate that for you."

            else ->
                "I'm AI Ish, your private on-device companion. I can help with:\n" +
                        "• Real-time knowledge (weather, crypto, news, sports)\n" +
                        "• Math calculations with step-by-step solving\n" +
                        "• Code assistance\n" +
                        "• Vision analysis (say 'what do you see?')\n\n" +
                        "What would you like to know?"
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
