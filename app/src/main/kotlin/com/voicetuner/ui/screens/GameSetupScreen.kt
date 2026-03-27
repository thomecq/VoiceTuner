package com.voicetuner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.voicetuner.model.GameConfig
import com.voicetuner.model.GameDifficulty
import com.voicetuner.model.GameType
import com.voicetuner.model.OctaveRange
import com.voicetuner.ui.theme.GradientEnd
import com.voicetuner.ui.theme.GradientStart
import com.voicetuner.ui.theme.Secondary
import com.voicetuner.ui.theme.TextSubtle
import com.voicetuner.viewmodel.GameViewModel

@Composable
fun GameSetupScreen(gameViewModel: GameViewModel) {
    var selectedType by remember { mutableStateOf(GameType.SINGLE_NOTES) }
    var selectedDifficulty by remember { mutableStateOf(GameDifficulty.EASY) }
    var selectedOctave by remember { mutableStateOf(OctaveRange.MIDDLE) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GradientStart, GradientEnd)))
        ) {
            Text("\uD83C\uDFAE", fontSize = 36.sp)
        }

        Text(
            text = stringResource(R.string.game),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Game type selection
        Text(
            text = stringResource(R.string.game_type),
            style = MaterialTheme.typography.titleMedium,
            color = TextSubtle
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GameTypeCard(
                title = stringResource(R.string.game_single_notes),
                emoji = "\uD83C\uDFB5",
                selected = selectedType == GameType.SINGLE_NOTES,
                onClick = { selectedType = GameType.SINGLE_NOTES },
                modifier = Modifier.weight(1f)
            )
            GameTypeCard(
                title = stringResource(R.string.game_chords),
                emoji = "\uD83C\uDFB6",
                selected = selectedType == GameType.CHORDS,
                onClick = { selectedType = GameType.CHORDS },
                modifier = Modifier.weight(1f)
            )
        }

        // Difficulty selection
        Text(
            text = stringResource(R.string.difficulty),
            style = MaterialTheme.typography.titleMedium,
            color = TextSubtle
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedDifficulty == GameDifficulty.EASY,
                onClick = { selectedDifficulty = GameDifficulty.EASY },
                label = {
                    Text(
                        stringResource(R.string.game_easy),
                        fontWeight = if (selectedDifficulty == GameDifficulty.EASY) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            FilterChip(
                selected = selectedDifficulty == GameDifficulty.STANDARD,
                onClick = { selectedDifficulty = GameDifficulty.STANDARD },
                label = {
                    Text(
                        stringResource(R.string.game_standard),
                        fontWeight = if (selectedDifficulty == GameDifficulty.STANDARD) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Secondary.copy(alpha = 0.15f),
                    selectedLabelColor = Secondary
                )
            )
        }

        // Octave range selection
        Text(
            text = stringResource(R.string.octave_range),
            style = MaterialTheme.typography.titleMedium,
            color = TextSubtle
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Row 1: single octaves
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(OctaveRange.LOW, OctaveRange.MIDDLE, OctaveRange.HIGH).forEach { range ->
                    FilterChip(
                        selected = selectedOctave == range,
                        onClick = { selectedOctave = range },
                        label = { Text(range.label, fontWeight = if (selectedOctave == range) FontWeight.SemiBold else FontWeight.Normal) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            // Row 2: two-octave ranges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(OctaveRange.LOW_MID, OctaveRange.MID_HIGH).forEach { range ->
                    FilterChip(
                        selected = selectedOctave == range,
                        onClick = { selectedOctave = range },
                        label = { Text(range.label, fontWeight = if (selectedOctave == range) FontWeight.SemiBold else FontWeight.Normal) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start button
        Button(
            onClick = {
                if (hasPermission) {
                    gameViewModel.startGame(GameConfig(selectedType, selectedDifficulty, selectedOctave))
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Text(
                if (hasPermission) stringResource(R.string.start_game) else stringResource(R.string.grant_permission),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GameTypeCard(
    title: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 32.sp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}
