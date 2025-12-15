# Fixes Log

## 2025-12-15: Fix Unit Test Null Safety
- **Error**: `ChatViewModelTest.kt:82:52 Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?`
- **Analysis**: `ChatViewModelTest.kt` accesses `streamingMessage.value` which appears to be `String?` (nullable). The test uses `.isEmpty()` or similar unsafe calls.
- **Fix**: Update `ChatViewModelTest.kt` to use null-safe assertions (e.g., `isNullOrEmpty()` or `== ""`).
- **Files**: `app/src/test/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModelTest.kt`
