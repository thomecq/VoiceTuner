package com.voicetuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicetuner.ui.navigation.AppNavigation
import com.voicetuner.ui.theme.VoiceTunerTheme
import com.voicetuner.viewmodel.GameViewModel
import com.voicetuner.viewmodel.PianoViewModel
import com.voicetuner.viewmodel.PitchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceTunerTheme {
                val pianoViewModel: PianoViewModel = viewModel()
                val pitchViewModel: PitchViewModel = viewModel()
                val gameViewModel: GameViewModel = viewModel()
                AppNavigation(pianoViewModel, pitchViewModel, gameViewModel)
            }
        }
    }
}
