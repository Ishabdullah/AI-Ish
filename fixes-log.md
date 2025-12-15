# Fixes Log

## 2025-12-15: Fix Unit Test Null Safety
- **Error**: `ChatViewModelTest.kt:82:52 Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?`
- **Analysis**: `ChatViewModelTest.kt` accesses `streamingMessage.value` which appears to be `String?` (nullable). The test uses `.isEmpty()` or similar unsafe calls.
- **Fix**: Update `ChatViewModelTest.kt` to use null-safe assertions (e.g., `isNullOrEmpty()` or `== ""`).
- **Files**: `app/src/test/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModelTest.kt`
- **Commit**: `8157de9`

## 2025-12-15: Fix Uncaught Coroutine Exceptions in Tests
- **Error**: `com.ishabdullah.aiish.ui.viewmodels.ChatViewModelTest > sendMessage adds user message and sets loading state FAILED` (and `initial state is correct FAILED`)
- **Exception**: `kotlinx.coroutines.test.UncaughtExceptionsBeforeTest`
- **Analysis**: The tests are failing due to uncaught exceptions in coroutines, likely triggered during `ChatViewModel` initialization. Specifically, `viewModelScope.launch` blocks in `init` (TTS init, Transcription collection) might be throwing exceptions that aren't handled or mocked correctly in the test environment, causing the test runner to fail before the test logic completes. The `UncaughtExceptionsBeforeTest` often indicates background jobs from `init` failing.
- **Fix**: 
    1.  Ensure all dependencies used in `init` blocks (`TTSManager`, `TranscriptionBroadcaster`) are properly mocked.
    2.  `TranscriptionBroadcaster` is a static/object dependency, which is hard to mock directly. We might need to wrap it or mock its Flow if possible, or ensure the ViewModel handles its potential absence/failure gracefully in tests.
    3.  Review `ChatViewModel` init block to ensure robust error handling for background tasks.
- **Files**: `app/src/test/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModelTest.kt`, `app/src/main/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModel.kt`