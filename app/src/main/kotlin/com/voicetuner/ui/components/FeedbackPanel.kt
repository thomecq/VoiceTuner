package com.voicetuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicetuner.R
import com.voicetuner.model.Accuracy
import com.voicetuner.model.PitchFeedback
import com.voicetuner.ui.theme.CorrectGreen
import com.voicetuner.ui.theme.CorrectGreenLight
import com.voicetuner.ui.theme.GradientEnd
import com.voicetuner.ui.theme.GradientStart
import com.voicetuner.ui.theme.TooHighRed
import com.voicetuner.ui.theme.TooHighRedLight
import com.voicetuner.ui.theme.TooLowBlue
import com.voicetuner.ui.theme.TooLowBlueLight
import com.voicetuner.ui.theme.TextSubtle

@Composable
fun FeedbackPanel(
    feedback: PitchFeedback,
    tolerance: Float,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (resultText, resultColor, resultBg) = when (feedback.accuracy) {
        Accuracy.CORRECT -> Triple(stringResource(R.string.correct), CorrectGreen, CorrectGreenLight)
        Accuracy.TOO_LOW -> Triple(stringResource(R.string.too_low), TooLowBlue, TooLowBlueLight)
        Accuracy.TOO_HIGH -> Triple(stringResource(R.string.too_high), TooHighRed, TooHighRedLight)
        Accuracy.NO_PITCH -> Triple("\u2014", TextSubtle, Color(0xFFEEEEEE))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ---- Hero result badge ----
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .shadow(12.dp, CircleShape, ambientColor = resultColor.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(resultBg, resultBg.copy(alpha = 0.6f))
                    )
                )
        ) {
            Text(
                text = when (feedback.accuracy) {
                    Accuracy.CORRECT -> "\u2713"
                    Accuracy.TOO_LOW -> "\u2193"
                    Accuracy.TOO_HIGH -> "\u2191"
                    Accuracy.NO_PITCH -> "?"
                },
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = resultColor,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = resultText,
            style = MaterialTheme.typography.headlineMedium,
            color = resultColor
        )

        // ---- Note comparison card ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteCircle(
                    label = stringResource(R.string.played_note),
                    note = feedback.targetNote.displayName,
                    bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    textColor = MaterialTheme.colorScheme.primary
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2192", fontSize = 22.sp, color = TextSubtle)
                    val centsSign = if (feedback.centsOffset > 0) "+" else ""
                    Text(
                        text = "${centsSign}${feedback.centsOffset.toInt()} ct",
                        style = MaterialTheme.typography.labelMedium,
                        color = resultColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                NoteCircle(
                    label = stringResource(R.string.your_note),
                    note = feedback.detectedNote?.displayName ?: "\u2014",
                    bgColor = resultColor.copy(alpha = 0.12f),
                    textColor = resultColor
                )
            }
        }

        // ---- Gauge ----
        PitchIndicator(
            centsOffset = feedback.centsOffset,
            tolerance = tolerance
        )

        // ---- Stats row ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val centsSign = if (feedback.centsOffset > 0) "+" else ""
            StatPill(
                value = "${centsSign}${"%.1f".format(feedback.semitonesOffset)}",
                label = stringResource(R.string.semitones),
                color = resultColor
            )
            StatPill(
                value = "${"%.0f".format(feedback.detectedFrequency)}",
                label = "Hz",
                color = TextSubtle
            )
        }

        // ---- Retry button ----
        Button(
            onClick = onTryAgain,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.6f)
                .background(
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Text(
                stringResource(R.string.try_again),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun NoteCircle(label: String, note: String, bgColor: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSubtle
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bgColor)
        ) {
            Text(
                text = note,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.7f)
        )
    }
}
