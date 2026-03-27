package com.voicetuner.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicetuner.R
import com.voicetuner.model.GameDifficulty
import com.voicetuner.model.GamePhase
import com.voicetuner.model.NoteScore
import com.voicetuner.model.RoundResult
import com.voicetuner.ui.components.PitchIndicator
import com.voicetuner.ui.theme.CorrectGreen
import com.voicetuner.ui.theme.CorrectGreenLight
import com.voicetuner.ui.theme.GoldStar
import com.voicetuner.ui.theme.GoldStarLight
import com.voicetuner.ui.theme.GradientEnd
import com.voicetuner.ui.theme.GradientStart
import com.voicetuner.ui.theme.Secondary
import com.voicetuner.ui.theme.TextSubtle
import com.voicetuner.ui.theme.TooHighRed
import com.voicetuner.ui.theme.WarningYellow
import com.voicetuner.viewmodel.GameViewModel
import kotlin.math.abs

@Composable
fun GamePlayScreen(gameViewModel: GameViewModel) {
    val gameState by gameViewModel.gameState.collectAsState()
    val isRecording by gameViewModel.isRecording.collectAsState()
    val currentPitch by gameViewModel.currentPitchResult.collectAsState()
    val chordNoteIndex by gameViewModel.currentChordNoteIndex.collectAsState()
    val replayUsed by gameViewModel.replayUsed.collectAsState()

    val round = gameState.rounds.getOrNull(gameState.currentRoundIndex)
    val totalRounds = gameState.config.roundCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: quit button + round + score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { gameViewModel.resetGame() }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.go_back),
                    tint = TextSubtle
                )
            }
            Text(
                text = stringResource(R.string.round_of, gameState.currentRoundIndex + 1, totalRounds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${gameState.totalScore} ${stringResource(R.string.points)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Progress dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            for (i in 0 until totalRounds) {
                val dotColor = when {
                    i < gameState.results.size -> {
                        val result = gameState.results[i]
                        val best = if (result.round.isChord) {
                            if (result.notePoints >= 20) NoteScore.GREAT else if (result.notePoints >= 10) NoteScore.CLOSE else NoteScore.MISS
                        } else {
                            result.noteScores.firstOrNull() ?: NoteScore.MISS
                        }
                        when (best) {
                            NoteScore.PERFECT -> GoldStar
                            NoteScore.GREAT -> CorrectGreen
                            NoteScore.GOOD -> WarningYellow
                            NoteScore.CLOSE -> Secondary
                            NoteScore.MISS -> TooHighRed
                        }
                    }
                    i == gameState.currentRoundIndex -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFE0DDEF)
                }
                val isCurrent = i == gameState.currentRoundIndex && gameState.phase != GamePhase.ROUND_RESULT

                val pulse = rememberInfiniteTransition(label = "dot_$i")
                val dotScale by pulse.animateFloat(
                    initialValue = if (isCurrent) 0.8f else 1f,
                    targetValue = if (isCurrent) 1.2f else 1f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "ds_$i"
                )

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        // Main content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (gameState.phase) {
                GamePhase.PLAYING_NOTE -> PlayingNoteState(
                    isChord = round?.isChord == true,
                    replayUsed = replayUsed,
                    onReplay = { gameViewModel.replayNote() }
                )
                GamePhase.COUNT_GUESS -> CountGuessState(
                    onGuess = { gameViewModel.submitCountGuess(it) }
                )
                GamePhase.SINGING -> SingingState(
                    isEasy = gameState.config.difficulty == GameDifficulty.EASY,
                    isChord = round?.isChord == true,
                    chordNoteIndex = chordNoteIndex,
                    chordSize = round?.targetNotes?.size ?: 1,
                    currentPitchCents = currentPitch?.let { pitch ->
                        val target = round?.targetNotes?.getOrNull(
                            if (round.isChord) chordNoteIndex else 0
                        )
                        if (target != null) {
                            com.voicetuner.audio.NoteFrequencies.centsFromNote(pitch.frequency, target)
                        } else 0f
                    } ?: 0f,
                    isRecording = isRecording
                )
                GamePhase.ROUND_RESULT -> {
                    val result = gameState.results.lastOrNull()
                    if (result != null) {
                        RoundResultState(
                            result = result,
                            streak = gameState.currentStreak,
                            onNext = { gameViewModel.nextRound() }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun PlayingNoteState(isChord: Boolean, replayUsed: Boolean, onReplay: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "note_pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "np_s"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .shadow(16.dp, CircleShape, ambientColor = GradientStart.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GradientStart, GradientEnd)))
        ) {
            Text(
                text = if (isChord) "\uD83C\uDFB6" else "\uD83C\uDFB5",
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(if (isChord) R.string.listen_chord else R.string.listen_now),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        if (isChord && !replayUsed) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onReplay,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(stringResource(R.string.replay))
            }
        }
    }
}

@Composable
private fun CountGuessState(onGuess: (Int) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.how_many_notes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(2, 3).forEach { count ->
                Button(
                    onClick = { onGuess(count) },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        "$count",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SingingState(
    isEasy: Boolean,
    isChord: Boolean,
    chordNoteIndex: Int,
    chordSize: Int,
    currentPitchCents: Float,
    isRecording: Boolean
) {
    val pulse = rememberInfiniteTransition(label = "sing")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "sa"
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
        // Pulsing recording indicator
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = 0.08f * dotAlpha))
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = dotAlpha))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isChord) {
            Text(
                text = stringResource(R.string.sing_note_of, chordNoteIndex + 1, chordSize),
                style = MaterialTheme.typography.titleMedium,
                color = Secondary,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = stringResource(R.string.sing_now),
                style = MaterialTheme.typography.titleMedium,
                color = Secondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Easy mode: show pitch indicator (without note name!)
        if (isEasy && isRecording) {
            PitchIndicator(
                centsOffset = currentPitchCents,
                tolerance = 25f
            )
        }
    }
}

@Composable
private fun RoundResultState(result: RoundResult, streak: Int, onNext: () -> Unit) {
    val bestScore = if (result.round.isChord) {
        if (result.notePoints + result.countPoints >= 30) NoteScore.PERFECT
        else if (result.notePoints + result.countPoints >= 20) NoteScore.GREAT
        else if (result.notePoints + result.countPoints >= 10) NoteScore.GOOD
        else NoteScore.MISS
    } else {
        result.noteScores.firstOrNull() ?: NoteScore.MISS
    }

    val (icon, iconBg, iconColor) = when (bestScore) {
        NoteScore.PERFECT -> Triple("\u2605", GoldStarLight, GoldStar)
        NoteScore.GREAT -> Triple("\u2605", CorrectGreenLight, CorrectGreen)
        NoteScore.GOOD -> Triple("\u2606", WarningYellow.copy(alpha = 0.2f), WarningYellow)
        NoteScore.CLOSE -> Triple("\u25CB", Secondary.copy(alpha = 0.15f), Secondary)
        NoteScore.MISS -> Triple("\u2717", TooHighRed.copy(alpha = 0.15f), TooHighRed)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Result icon
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith fadeOut() },
            label = "result_icon"
        ) { ic ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .shadow(12.dp, CircleShape, ambientColor = iconColor.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                Text(ic, fontSize = 40.sp, color = iconColor)
            }
        }

        Text(
            text = bestScore.label,
            style = MaterialTheme.typography.headlineMedium,
            color = iconColor,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "+${result.totalScore} ${stringResource(R.string.points)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (result.streakBonus > 0) {
            Text(
                text = stringResource(R.string.streak_bonus, result.streakBonus),
                style = MaterialTheme.typography.bodyMedium,
                color = GoldStar,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Note details — NOW we can show note names
        if (result.round.isChord) {
            // Chord results
            if (result.countCorrect != null) {
                Text(
                    text = stringResource(if (result.countCorrect) R.string.note_count_correct else R.string.note_count_wrong),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (result.countCorrect) CorrectGreen else TooHighRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
            result.round.targetNotes.forEachIndexed { i, target ->
                val detected = result.detectedNotes.getOrNull(i)
                val cents = result.centsOffsets.getOrNull(i) ?: 0f
                val score = result.noteScores.getOrNull(i) ?: NoteScore.MISS
                val scoreIcon = if (score.points >= 20) "\u2713" else if (score.points >= 10) "~" else "\u2717"
                val scoreColor = if (score.points >= 20) CorrectGreen else if (score.points >= 10) WarningYellow else TooHighRed

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$scoreIcon ${target.displayName} \u2192 ${detected?.displayName ?: "\u2014"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scoreColor
                    )
                    Text(
                        "${if (cents > 0) "+" else ""}${cents.toInt()} ct",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtle
                    )
                }
            }
        } else {
            val target = result.round.targetNotes[0]
            val detected = result.detectedNotes.firstOrNull()
            val cents = result.centsOffsets.firstOrNull() ?: 0f

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.played), style = MaterialTheme.typography.labelMedium, color = TextSubtle)
                    Text(target.displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("\u2192", fontSize = 20.sp, color = TextSubtle)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.sung), style = MaterialTheme.typography.labelMedium, color = TextSubtle)
                    Text(
                        detected?.displayName ?: "\u2014",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${if (cents > 0) "+" else ""}${cents.toInt()} ct",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtle
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(52.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Text(stringResource(R.string.next_round), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
    }
}
