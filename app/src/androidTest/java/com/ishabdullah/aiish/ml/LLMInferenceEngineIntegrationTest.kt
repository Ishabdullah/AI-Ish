package com.ishabdullah.aiish.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for LLMInferenceEngine JNI layer.
 * These tests verify the native methods can be called and return expected results for basic scenarios.
 */
@RunWith(AndroidJUnit4::class)
class LLMInferenceEngineIntegrationTest {

    private lateinit var llmInferenceEngine: LLMInferenceEngine
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        llmInferenceEngine = LLMInferenceEngine()
    }

    @Test
    fun `nativeLoadModel returns error for non-existent model path`() {
        val nonExistentPath = "/path/to/non_existent_model.gguf"
        val modelFile = File(nonExistentPath)

        // Attempt to load a non-existent model
        val success = runBlocking {
            llmInferenceEngine.loadModel(modelFile.absolutePath, 0, 0)
        }

        // Expect it to fail
        assertFalse("Loading a non-existent model should fail", success)
        assertFalse("Model should not be loaded", llmInferenceEngine.isLoaded())
    }

    @Test
    fun `nativeLoadModel and nativeFree handle valid cycle (mocked)`() {
        // This test cannot fully validate actual model loading without a real model file.
        // Instead, it verifies that calling load and then free does not crash and
        // sets the isModelLoaded flag correctly.

        // A dummy file to pass the existence check in loadModel
        val dummyModelFile = File(appContext.filesDir, "dummy_model.gguf")
        dummyModelFile.createNewFile() // Create a dummy file

        // Mock the nativeLoadModel to return success for this path
        // This is a limitation: we can't truly mock native function return values easily from Kotlin.
        // For actual native behavior testing, you'd need C++ side mocks or a proper test model.
        // For now, we assume nativeLoadModel will fail for the dummy file as it's not a valid GGUF.

        val success = runBlocking {
            llmInferenceEngine.loadModel(dummyModelFile.absolutePath, 0, 0)
        }
        
        // Assert that loading failed, because it's a dummy file and not a real GGUF
        assertFalse("Loading a dummy model should fail", success)
        assertFalse("Model should not be loaded after dummy load attempt", llmInferenceEngine.isLoaded())

        // Ensure release doesn't crash even if load failed
        llmInferenceEngine.release()
        assertFalse("Model should not be loaded after release", llmInferenceEngine.isLoaded())

        dummyModelFile.delete() // Clean up dummy file
    }
}
