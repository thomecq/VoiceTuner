package com.voicetuner.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicetuner.R
import com.voicetuner.model.GameType
import com.voicetuner.model.NoteScore
import com.voicetuner.ui.theme.CorrectGreen
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
fun GameResultScreen(gameViewModel: GameViewModel) {
    val gameState by gameViewModel.gameState.collectAsState()

    val results = gameState.results
    val totalScore = gameState.totalScore
    val maxScore = if (gameState.config.type == GameType.SINGLE_NOTES) {
        gameState.config.roundCount * 30
    } else {
        gameState.config.roundCount * 40
    }
    val ratio = if (maxScore > 0) totalScore.toFloat() / maxScore else 0f

    val trophy = when {
        ratio >= 0.8f -> "\uD83C\uDFC6"
        ratio >= 0.6f -> "\uD83E\uDD48"
        ratio >= 0.4f -> "\uD83E\uDD49"
        else -> "\uD83D\uDCAA"
    }

    val accurateNotes = results.flatMap { it.noteScores }.count { it.points >= 20 }
    val totalNotes = results.flatMap { it.noteScores }.size
    val avgError = results.flatMap { it.centsOffsets }.map { abs(it) }.let {
        if (it.isNotEmpty()) it.average().toInt() else 0
    }
    val bestResult = results.flatMap { r ->
        r.round.targetNotes.zip(r.centsOffsets)
    }.minByOrNull { abs(it.second) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Trophy
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .shadow(16.dp, CircleShape, ambientColor = GoldStar.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(GoldStarLight)
        ) {
            Text(trophy, fontSize = 48.sp)
        }

        Text(
            text = stringResource(R.string.final_score),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Big score
        Text(
            text = "$totalScore / $maxScore",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Progress dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            results.forEach { result ->
                val best = if (result.round.isChord) {
                    if (result.notePoints >= 20) NoteScore.GREAT else if (result.notePoints >= 10) NoteScore.CLOSE else NoteScore.MISS
                } else {
                    result.noteScores.firstOrNull() ?: NoteScore.MISS
                }
                val dotColor = when (best) {
                    NoteScore.PERFECT -> GoldStar
                    NoteScore.GREAT -> CorrectGreen
                    NoteScore.GOOD -> WarningYellow
                    NoteScore.CLOSE -> Secondary
                    NoteScore.MISS -> TooHighRed
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = stringResource(R.string.accurate_notes),
                value = "$accurateNotes / $totalNotes",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.avg_error),
                value = "$avgError ct",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = stringResource(R.string.best_note),
                value = bestResult?.let { "${it.first.displayName} (${if (it.second > 0) "+" else ""}${it.second.toInt()} ct)" } ?: "\u2014",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.longest_streak),
                value = "${gameState.bestStreak}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Button(
            onClick = {
                gameViewModel.resetGame()
                gameViewModel.startGame(gameState.config)
            },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Text(stringResource(R.string.play_again), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }

        OutlinedButton(
            onClick = { gameViewModel.resetGame() },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.go_back), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSubtle)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
