# Fixes Log

## 2025-12-15: Fix Unit Test Null Safety
- **Error**: `ChatViewModelTest.kt:82:52 Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?`
- **Analysis**: `ChatViewModelTest.kt` accesses `streamingMessage.value` which appears to be `String?` (nullable). The test uses `.isEmpty()` or similar unsafe calls.
- **Fix**: Update `ChatViewModelTest.kt` to use null-safe assertions (e.g., `isNullOrEmpty()` or `== ""`).
- **Files**: `app/src/test/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModelTest.kt`
- **Commit**: `8157de9`

## 2025-12-15: Fix Unnecessary Mockito Stubbings in Test
- **Error**: `UnnecessaryStubbingException` on test `unnecessary Mockito stubbings FAILED`
- **Root Cause**: Mock stubs were set up for `conversationDatabase.conversationDao()` and `conversationDao.getAllConversations()` but these stubs were never actually used during test execution. The test creates `ChatRepository` directly with `conversationDao`, bypassing the `conversationDatabase` mock entirely, causing Mockito's strict mode to flag the unused stubs.
- **Fix**: 
  1. Removed the unused `conversationDatabase` mock field and its import
  2. Removed the unused stub setup for `conversationDatabase.conversationDao()`
  3. Removed the unused stub setup for `conversationDao.getAllConversations()`
  4. Kept the direct initialization of `ChatRepository` with the mocked `conversationDao`
- **Files**: `app/src/test/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModelTest.kt`
- **Commit**: `8a4d624`

## 2025-12-15: Fix Suspend Function Call in Test Setup
- **Error**: `ChatViewModelTest.kt:69:27 Suspend function 'initialize' should be called only from a coroutine or another suspend function`
- **Root Cause**: The test setup method was attempting to mock `ttsManager.initialize()` which is a suspend function. Suspend functions cannot be called from synchronous methods like `@Before` setup().
- **Fix**: Removed the mock stub `when(ttsManager.initialize()).thenReturn(true)` from the setup() method. The mocked ttsManager is already provided to the ViewModel constructor and will be used correctly during initialization within the test's coroutine context (testScope.runTest).
- **Files**: `app/src/test/java/com/ishabdullah/aiish/ui/viewmodels/ChatViewModelTest.kt`
- **Commit**: `658f776`