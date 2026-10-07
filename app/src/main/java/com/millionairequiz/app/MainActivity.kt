package com.millionairequiz.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.millionairequiz.app.game.GameViewModel
import com.millionairequiz.app.game.Screen
import com.millionairequiz.app.game.UiState
import com.millionairequiz.app.ui.AudienceDialog
import com.millionairequiz.app.ui.GameOverScreen
import com.millionairequiz.app.ui.GameScreen
import com.millionairequiz.app.ui.LadderDialog
import com.millionairequiz.app.ui.MessageDialog
import com.millionairequiz.app.ui.MillionaireTheme
import com.millionairequiz.app.ui.Palette
import com.millionairequiz.app.ui.PhoneDialog
import com.millionairequiz.app.ui.SettingsDialog
import com.millionairequiz.app.ui.SetupScreen
import com.millionairequiz.app.ui.WalkAwayDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark background throughout, so always use light status/navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            MillionaireTheme {
                val vm: GameViewModel = viewModel()
                val state by vm.state.collectAsState()
                MillionaireApp(state, vm)
            }
        }
    }
}

@Composable
fun MillionaireApp(s: UiState, vm: GameViewModel) {
    BackHandler(enabled = s.screen != Screen.SETUP) {
        if (s.screen == Screen.GAME) vm.requestWalkAway() else vm.newTopic()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.background)
            .safeDrawingPadding()
    ) {
        when (s.screen) {
            Screen.SETUP -> SetupScreen(s, vm)
            Screen.GAME -> GameScreen(s, vm)
            Screen.GAME_OVER -> s.result?.let { GameOverScreen(it, s.topic, vm) }
        }
    }

    // Overlays
    if (s.showSettings) {
        SettingsDialog(
            currentKey = s.apiKey,
            currentModel = s.model,
            currentCheapEasy = s.cheapEasy,
            onSave = vm::saveSettings,
            onDismiss = vm::closeSettings,
        )
    }
    if (s.showLadder) LadderDialog(s.level) { vm.setLadderVisible(false) }
    if (s.confirmWalkAway) WalkAwayDialog(s.level, onConfirm = vm::walkAway, onDismiss = vm::dismissWalkAway)
    val phone = s.phone
    if (s.showPhone && phone != null) PhoneDialog(phone, onDismiss = vm::dismissPhone)
    val audience = s.audience
    if (s.showAudience && audience != null) AudienceDialog(audience, s.removed, onDismiss = vm::dismissAudience)
    s.lifelineError?.let { MessageDialog("Phone a Friend", it, onDismiss = vm::dismissLifelineError) }
}
