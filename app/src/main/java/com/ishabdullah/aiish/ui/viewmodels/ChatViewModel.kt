package com.ishabdullah.aiish.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ishabdullah.aiish.audio.TTSManager
import com.ishabdullah.aiish.domain.models.Message
import com.ishabdullah.aiish.domain.models.MessageRole
import com.ishabdullah.aiish.ml.LLMInferenceEngine
import com.ishabdullah.aiish.ml.TokenStreamHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Chat ViewModel - Manages chat conversation state with real LLM inference and TTS
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _shouldOpenCamera = MutableStateFlow(false)
    val shouldOpenCamera: StateFlow<Boolean> = _shouldOpenCamera.asStateFlow()

    // LLM inference components
    private val llmEngine = LLMInferenceEngine()
    private val streamHandler = TokenStreamHandler()

    // TTS component
    private val ttsManager = TTSManager(application)
    private val _ttsEnabled = MutableStateFlow(false)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    // Streaming state
    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage: StateFlow<String?> = _streamingMessage.asStateFlow()

    init {
        Timber.d("ChatViewModel initialized")

        // Initialize TTS
        viewModelScope.launch {
            val success = ttsManager.initialize()
            if (success) {
                Timber.i("TTS initialized successfully")
            } else {
                Timber.w("TTS initialization failed")
            }
        }
    }

    /**
     * Send message and get AI response with streaming
     */
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
                _streamingMessage.value = ""

                // Generate AI response with streaming
                val aiResponse = if (llmEngine.isLoaded()) {
                    generateStreamingResponse(content)
                } else {
                    Timber.w("LLM not loaded, using fallback")
                    generateFallbackResponse(content)
                }

                val assistantMessage = Message(
                    content = aiResponse,
                    role = MessageRole.ASSISTANT
                )
                _messages.value = _messages.value + assistantMessage

                // Speak response if TTS is enabled
                if (_ttsEnabled.value && aiResponse.isNotBlank()) {
                    speakResponse(aiResponse)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error sending message")
                val errorMessage = Message(
                    content = "Sorry, I encountered an error: ${e.message}",
                    role = MessageRole.ASSISTANT
                )
                _messages.value = _messages.value + errorMessage

                // Speak error if TTS is enabled
                if (_ttsEnabled.value) {
                    speakResponse(errorMessage.content)
                }
            } finally {
                _isLoading.value = false
                _streamingMessage.value = null
            }
        }
    }

    /**
     * Generate response using actual LLM with token streaming
     */
    private suspend fun generateStreamingResponse(userMessage: String): String {
        try {
            // Build prompt with context
            val prompt = buildPrompt(userMessage)

            // Start streaming generation
            val tokenFlow = llmEngine.generateStream(
                prompt = prompt,
                maxTokens = 512,
                temperature = 0.7f,
                topP = 0.9f,
                stopSequences = listOf("\n\nUser:", "\n\nHuman:", "<|end|>", "</s>")
            )

            // Collect streaming tokens and update UI
            val fullResponse = StringBuilder()
            tokenFlow.collect { token ->
                fullResponse.append(token)
                _streamingMessage.value = fullResponse.toString()
            }

            return fullResponse.toString().trim()

        } catch (e: Exception) {
            Timber.e(e, "Error during streaming generation")
            return "I encountered an error while generating a response. Please try again."
        }
    }

    /**
     * Build prompt with conversation context
     */
    private fun buildPrompt(userMessage: String): String {
        val context = _messages.value.takeLast(5).joinToString("\n") { msg ->
            when (msg.role) {
                MessageRole.USER -> "User: ${msg.content}"
                MessageRole.ASSISTANT -> "Assistant: ${msg.content}"
                MessageRole.SYSTEM -> "System: ${msg.content}"
            }
        }

        return """You are Ish, a helpful AI assistant running entirely on-device with complete privacy.
You are knowledgeable, concise, and friendly.

${if (context.isNotEmpty()) "Conversation context:\n$context\n\n" else ""}User: $userMessage
Assistant:"""
    }

    /**
     * Fallback response when LLM is not loaded
     */
    private fun generateFallbackResponse(userMessage: String): String {
        return when {
            userMessage.lowercase().contains("hello") || userMessage.lowercase().contains("hey") ->
                "Hello! I'm Ish, your private AI companion. However, my language model isn't loaded yet. Please download a model first."

            else ->
                "I'm AI Ish, your private on-device companion. My language model isn't loaded yet. Please download a model from the dashboard to start chatting!"
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

    /**
     * Load LLM model (called after download completes)
     */
    fun loadModel(modelPath: String, contextSize: Int = 4096, gpuLayers: Int = 0) {
        viewModelScope.launch {
            try {
                Timber.i("Loading model from: $modelPath")
                _isLoading.value = true

                val success = llmEngine.loadModel(modelPath, contextSize, gpuLayers)

                if (success) {
                    Timber.i("Model loaded successfully!")
                    // Add system message
                    val systemMessage = Message(
                        content = "Model loaded! I'm ready to chat. How can I help you?",
                        role = MessageRole.ASSISTANT
                    )
                    _messages.value = _messages.value + systemMessage
                } else {
                    Timber.e("Failed to load model")
                    val errorMessage = Message(
                        content = "Failed to load model. Please try downloading again.",
                        role = MessageRole.ASSISTANT
                    )
                    _messages.value = _messages.value + errorMessage
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading model")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    // =========================================================================
    // TTS Methods
    // =========================================================================

    /**
     * Speak assistant response using TTS
     */
    private fun speakResponse(text: String) {
        viewModelScope.launch {
            try {
                ttsManager.speak(text)
            } catch (e: Exception) {
                Timber.e(e, "Error speaking response")
            }
        }
    }

    /**
     * Enable TTS
     */
    fun enableTTS() {
        _ttsEnabled.value = true
        Timber.i("TTS enabled")
    }

    /**
     * Disable TTS
     */
    fun disableTTS() {
        _ttsEnabled.value = false
        ttsManager.stop()
        Timber.i("TTS disabled")
    }

    /**
     * Toggle TTS on/off
     */
    fun toggleTTS() {
        if (_ttsEnabled.value) {
            disableTTS()
        } else {
            enableTTS()
        }
    }

    /**
     * Stop current TTS playback
     */
    fun stopTTS() {
        ttsManager.stop()
    }

    /**
     * Set TTS speech rate
     */
    fun setTTSSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
    }

    /**
     * Set TTS pitch
     */
    fun setTTSPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }

    /**
     * Get TTS speaking state
     */
    fun isTTSSpeaking(): Boolean {
        return ttsManager.isSpeaking.value
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.release()
        llmEngine.release()
    }
}
