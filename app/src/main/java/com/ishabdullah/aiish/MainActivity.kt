package com.ishabdullah.aiish

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Application
import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ishabdullah.aiish.data.local.ConversationDatabase
import com.ishabdullah.aiish.data.local.preferences.PreferencesManager
import com.ishabdullah.aiish.data.repository.ChatRepository
import com.ishabdullah.aiish.ml.LLMInferenceEngine
import com.ishabdullah.aiish.ui.screens.*
import com.ishabdullah.aiish.ui.theme.AIIshTheme
import com.ishabdullah.aiish.ui.viewmodels.ChatViewModel
import timber.log.Timber

/**
 * Main Activity - Entry point for AI Ish
 */
class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            Timber.d("Permission ${entry.key} = ${entry.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        setContent {
            AIIshTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiIshNavigation()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Composable
fun AiIshNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val application = context.applicationContext as Application // Get Application instance

    // Instantiate ChatRepository dependencies
    val conversationDatabase = remember { ConversationDatabase.getDatabase(application) }
    val chatRepository = remember { ChatRepository(conversationDatabase.conversationDao()) }

    // Instantiate LLMInferenceEngine
    val llmInferenceEngine = remember { LLMInferenceEngine() }

    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(application, chatRepository, llmInferenceEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    val hasCompletedOnboarding by preferencesManager.hasCompletedOnboarding.collectAsState(initial = false)

    val startDestination = if (hasCompletedOnboarding) "dashboard" else "model_download"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("model_download") {
            ModelDownloadScreen(
                onDownloadComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("model_download") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate("camera") }
            )
        }
        composable("camera") {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                preferencesManager = preferencesManager
            )
        }
    }
}
