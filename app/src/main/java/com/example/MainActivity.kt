package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EditScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScansLibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartActionsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AuraScanTheme
import com.example.ui.viewmodel.AuraScanViewModel

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.screens.OnboardingScreen

class MainActivity : ComponentActivity() {

    private val viewModel: AuraScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads (AdMob) Rewarded Ads
        com.example.utils.RewardedAdManager.initialize(this)

        setContent {
            val themeMode by viewModel.appThemeMode.collectAsState()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            AuraScanTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuraScanApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AuraScanApp(viewModel: AuraScanViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val showAiConfirmation by viewModel.showAiConfirmation.collectAsState()
    val rewardToast by viewModel.rewardToast.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(rewardToast) {
        rewardToast?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.dismissRewardToast()
        }
    }

    if (!hasCompletedOnboarding) {
        OnboardingScreen(viewModel = viewModel)
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "home" -> HomeScreen(viewModel = viewModel)
                "camera" -> CameraScreen(viewModel = viewModel)
                "edit" -> EditScreen(viewModel = viewModel)
                "analysis" -> AnalysisScreen(viewModel = viewModel)
                "smart_actions" -> SmartActionsScreen(viewModel = viewModel)
                "scans" -> ScansLibraryScreen(viewModel = viewModel)
                "scan_detail" -> com.example.ui.screens.ScanDetailScreen(viewModel = viewModel)
                "tools" -> ToolsScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
                else -> HomeScreen(viewModel = viewModel)
            }
        }
    }

    if (showAiConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingAiAction() },
            title = { Text("Send to Gemini AI?", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { Text("You enabled 'Ask before sending to AI'. This will securely send this document to Google Gemini for smart analysis.") },
            confirmButton = {
                Button(onClick = { viewModel.confirmPendingAiAction() }) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingAiAction() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
