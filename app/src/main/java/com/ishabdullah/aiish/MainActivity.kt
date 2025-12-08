package com.ishabdullah.aiish

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ishabdullah.aiish.ui.screens.ChatScreen
import com.ishabdullah.aiish.ui.screens.DashboardScreen
import com.ishabdullah.aiish.ui.screens.SettingsScreen
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
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
