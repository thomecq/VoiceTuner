package com.voicetuner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voicetuner.R
import com.voicetuner.ui.components.FeedbackPanel
import com.voicetuner.ui.components.PianoKeyboard
import com.voicetuner.ui.components.PitchIndicator
import com.voicetuner.ui.theme.GradientEnd
import com.voicetuner.ui.theme.GradientStart
import com.voicetuner.ui.theme.Secondary
import com.voicetuner.ui.theme.Tertiary
import com.voicetuner.ui.theme.TextSubtle
import com.voicetuner.viewmodel.PianoViewModel
import com.voicetuner.viewmodel.PitchViewModel

@Composable
fun MainScreen(
    pianoViewModel: PianoViewModel,
    pitchViewModel: PitchViewModel
) {
    val pressedKeys by pianoViewModel.pressedKeys.collectAsState()
    val lastPlayedNote by pianoViewModel.lastPlayedNote.collectAsState()
    val playCounter by pianoViewModel.playCounter.collectAsState()
    val isChordMode by pianoViewModel.isChordMode.collectAsState()
    val isRecording by pitchViewModel.isRecording.collectAsState()
    val isWaiting by pitchViewModel.isWaitingToRecord.collectAsState()
    val currentFeedback by pitchViewModel.currentFeedback.collectAsState()
    val currentPitchResult by pitchViewModel.currentPitchResult.collectAsState()
    val tolerance by pitchViewModel.toleranceCents.collectAsState()

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(playCounter) {
        if (playCounter == 0) return@LaunchedEffect
        val note = lastPlayedNote ?: return@LaunchedEffect
        if (hasPermission) pitchViewModel.onNotePlayed(note)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ---- Top content area ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Subtle decorative background circles
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0x08_6C63FF),
                    radius = size.width * 0.6f,
                    center = center.copy(x = size.width * 0.8f, y = size.height * 0.2f)
                )
                drawCircle(
                    color = Color(0x06_4ECDC4),
                    radius = size.width * 0.4f,
                    center = center.copy(x = size.width * 0.15f, y = size.height * 0.7f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    !hasPermission -> PermissionRequest(
                        onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                    isWaiting -> WaitingState(noteName = lastPlayedNote?.displayName ?: "")
                    isRecording -> RecordingState(
                        noteName = currentPitchResult?.closestNote?.displayName,
                        frequency = currentPitchResult?.frequency,
                        centsOffset = currentFeedback?.centsOffset ?: 0f,
                        tolerance = tolerance
                    )
                    currentFeedback != null -> FeedbackPanel(
                        feedback = currentFeedback!!,
                        tolerance = tolerance,
                        onTryAgain = { pitchViewModel.clearFeedback() }
                    )
                    else -> IdleState()
                }
            }
        }

        // ---- Mode selector ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            ModeChip(
                label = stringResource(R.string.single_note),
                selected = !isChordMode,
                onClick = { if (isChordMode) pianoViewModel.toggleChordMode() }
            )
            ModeChip(
                label = stringResource(R.string.chord_mode),
                selected = isChordMode,
                onClick = { if (!isChordMode) pianoViewModel.toggleChordMode() }
            )
        }

        // ---- Piano with dark surround ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF363360), Color(0xFF2D2B55))
                    )
                )
                .padding(top = 6.dp, bottom = 8.dp)
        ) {
            PianoKeyboard(
                notes = pianoViewModel.allNotes,
                pressedKeys = pressedKeys,
                onKeyPress = { note -> pianoViewModel.onKeyPress(note) }
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
        },
        shape = RoundedCornerShape(20.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = Color.Transparent,
            labelColor = TextSubtle
        )
    )
}

// ---- States ----

@Composable
private fun IdleState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Text(text = "\uD83C\uDFB9", fontSize = 36.sp)
        }
        Text(
            text = stringResource(R.string.tap_key_to_start),
            style = MaterialTheme.typography.titleMedium,
            color = TextSubtle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WaitingState(noteName: String) {
    val pulse = rememberInfiniteTransition(label = "w")
    val s by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "ws"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .scale(s)
                .shadow(16.dp, CircleShape, ambientColor = GradientStart.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GradientStart, GradientEnd)))
        ) {
            Text(
                text = noteName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = stringResource(R.string.get_ready),
            style = MaterialTheme.typography.titleLarge,
            color = Secondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RecordingState(
    noteName: String?,
    frequency: Float?,
    centsOffset: Float,
    tolerance: Float
) {
    val pulse = rememberInfiniteTransition(label = "r")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "ra"
    )
    val ringScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "rs"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Listening badge with pulsing ring
        Box(contentAlignment = Alignment.Center) {
            // Outer pulse ring
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = 0.08f * dotAlpha))
            )
            // Inner dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = dotAlpha))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.listening),
            style = MaterialTheme.typography.titleMedium,
            color = Secondary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = noteName != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Big detected note
                AnimatedContent(
                    targetState = noteName ?: "",
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "note_anim"
                ) { name ->
                    Text(
                        text = name,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (frequency != null) {
                    Text(
                        text = "${"%.1f".format(frequency)} Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtle
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PitchIndicator(
                    centsOffset = centsOffset,
                    tolerance = tolerance
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Secondary.copy(alpha = 0.1f))
        ) {
            Text(text = "\uD83C\uDF99\uFE0F", fontSize = 36.sp)
        }
        Text(
            text = stringResource(R.string.microphone_permission_needed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}
