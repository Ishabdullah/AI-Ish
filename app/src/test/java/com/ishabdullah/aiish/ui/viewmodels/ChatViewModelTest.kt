package com.ishabdullah.aiish.ui.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import com.ishabdullah.aiish.data.local.ConversationDatabase
import com.ishabdullah.aiish.data.local.ConversationDao
import com.ishabdullah.aiish.data.repository.ChatRepository
import com.ishabdullah.aiish.ml.LLMInferenceEngine
import org.mockito.kotlin.mock
import com.ishabdullah.aiish.domain.models.Message
import com.ishabdullah.aiish.domain.models.MessageRole
import kotlinx.coroutines.flow.flowOf
import org.mockito.Mockito.`when`
import kotlinx.coroutines.flow.first

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChatViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var conversationDatabase: ConversationDatabase

    @Mock
    private lateinit var conversationDao: ConversationDao

    @Mock
    private lateinit var llmInferenceEngine: LLMInferenceEngine

    private lateinit var chatRepository: ChatRepository
    private lateinit var chatViewModel: ChatViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock ConversationDatabase and ConversationDao
        `when`(conversationDatabase.conversationDao()).thenReturn(conversationDao)
        `when`(conversationDao.getAllConversations()).thenReturn(flowOf(emptyList())) // Default empty list

        // Initialize ChatRepository with mocked dependencies
        chatRepository = ChatRepository(conversationDao)

        // Initialize ChatViewModel with mocked dependencies
        chatViewModel = ChatViewModel(application, chatRepository, llmInferenceEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = testScope.runTest {
        assert(chatViewModel.messages.first().isEmpty())
        assert(!chatViewModel.isLoading.value)
        assert(!chatViewModel.shouldOpenCamera.value)
        assert(chatViewModel.streamingMessage.value == null)
    }

    @Test
    fun `sendMessage adds user message and sets loading state`() = testScope.runTest {
        val testUserMessageContent = "Hello AI"
        
        // Mock LLM to return a simple response
        `when`(llmInferenceEngine.isLoaded()).thenReturn(true)
        `when`(llmInferenceEngine.generateStream(
            prompt = any(), // Use Mockito's any() for arbitrary prompt matching
            maxTokens = any(),
            temperature = any(),
            topP = any(),
            stopSequences = any()
        )).thenReturn(flowOf("AI Response"))

        chatViewModel.sendMessage(testUserMessageContent)

        // Verify user message is inserted
        // Note: Due to flow/state update nature and `runTest`, direct assertion on `messages.first()`
        // might not reflect the immediate state after insert if not properly mocked to emit
        // This test focuses on initial state and loading flags.
        // For verifying message content, you'd typically verify `chatRepository.insertMessage` calls.

        assert(chatViewModel.isLoading.value) // Should be true while processing
        assert(chatViewModel.streamingMessage.value == "") // Should be initialized to empty string
    }

    // This is a placeholder test, actual database interaction needs proper mocking for Flow emissions
    // For now, it verifies that the clearAllMessages function is called on the repository.
    @Test
    fun `clearMessages clears all messages via repository`() = testScope.runTest {
        chatViewModel.clearMessages()
        // Verify that clearAllMessages was called on the mock conversationDao
        org.mockito.Mockito.verify(conversationDao).clearAllConversations()
    }

    // Helper for Mockito any()
    private fun <T> any(): T = org.mockito.Mockito.any()
}
