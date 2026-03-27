package com.voicetuner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicetuner.ui.theme.CorrectGreen
import com.voicetuner.ui.theme.TextSubtle
import com.voicetuner.ui.theme.TooHighRed
import com.voicetuner.ui.theme.TooLowBlue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PitchIndicator(
    centsOffset: Float,
    tolerance: Float,
    modifier: Modifier = Modifier
) {
    val maxCents = 50f
    val normalizedOffset by animateFloatAsState(
        targetValue = (centsOffset / maxCents).coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "pitch_offset"
    )

    val accuracyColor = when {
        abs(centsOffset) <= tolerance -> CorrectGreen
        centsOffset < 0 -> TooLowBlue
        else -> TooHighRed
    }

    val glowColor = accuracyColor.copy(alpha = 0.25f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // The gauge canvas — semicircle only, no text inside
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val centerX = size.width / 2f
            val bottomY = size.height - 8f
            val radius = (size.width * 0.40f).coerceAtMost(bottomY - 12f)
            val arcThickness = 18f
            val outerGlowThickness = 28f

            val arcStartAngle = 195f
            val arcSweepAngle = 150f

            val arcSize = Size(radius * 2, radius * 2)
            val arcTopLeft = Offset(centerX - radius, bottomY - radius)

            // — Outer glow ring —
            drawArc(
                color = glowColor,
                startAngle = arcStartAngle,
                sweepAngle = arcSweepAngle,
                useCenter = false,
                topLeft = Offset(arcTopLeft.x - 5f, arcTopLeft.y - 5f),
                size = Size(arcSize.width + 10f, arcSize.height + 10f),
                style = Stroke(width = outerGlowThickness, cap = StrokeCap.Round)
            )

            // — Background track —
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFE0DDEF),
                        Color(0xFFEAE8F3),
                        Color(0xFFE0DDEF)
                    )
                ),
                startAngle = arcStartAngle,
                sweepAngle = arcSweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcThickness, cap = StrokeCap.Round)
            )

            // — Colored segments on the arc —
            val toleranceNorm = tolerance / maxCents
            val warnNorm = ((tolerance + 15f) / maxCents).coerceAtMost(1f)

            // Left blue zone (too low)
            drawArc(
                color = TooLowBlue.copy(alpha = 0.35f),
                startAngle = arcStartAngle,
                sweepAngle = arcSweepAngle * (0.5f - warnNorm),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcThickness, cap = StrokeCap.Round)
            )

            // Right red zone (too high)
            val rightStart = arcStartAngle + arcSweepAngle * (0.5f + warnNorm)
            drawArc(
                color = TooHighRed.copy(alpha = 0.35f),
                startAngle = rightStart,
                sweepAngle = arcSweepAngle * (0.5f - warnNorm),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcThickness, cap = StrokeCap.Round)
            )

            // Center green zone (correct)
            val greenStart = arcStartAngle + arcSweepAngle * (0.5f - toleranceNorm)
            val greenSweep = arcSweepAngle * toleranceNorm * 2f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        CorrectGreen.copy(alpha = 0.2f),
                        CorrectGreen.copy(alpha = 0.55f),
                        CorrectGreen.copy(alpha = 0.2f)
                    )
                ),
                startAngle = greenStart,
                sweepAngle = greenSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = arcThickness, cap = StrokeCap.Butt)
            )

            // — Tick marks —
            drawTicks(centerX, bottomY, radius, arcThickness, arcStartAngle, arcSweepAngle)

            // — Needle —
            val needleAngleDeg = arcStartAngle + arcSweepAngle * (0.5f + normalizedOffset / 2f)
            val needleAngleRad = needleAngleDeg * PI.toFloat() / 180f
            val needleLen = radius * 0.68f

            val tipX = centerX + needleLen * cos(needleAngleRad)
            val tipY = bottomY + needleLen * sin(needleAngleRad)

            // Needle shadow
            drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = Offset(centerX + 1.5f, bottomY + 1.5f),
                end = Offset(tipX + 1.5f, tipY + 1.5f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Needle body — tapered via Path
            val needleBaseHalf = 4f
            val perpX = -sin(needleAngleRad) * needleBaseHalf
            val perpY = cos(needleAngleRad) * needleBaseHalf
            val needlePath = Path().apply {
                moveTo(centerX + perpX, bottomY + perpY)
                lineTo(centerX - perpX, bottomY - perpY)
                lineTo(tipX, tipY)
                close()
            }
            drawPath(needlePath, accuracyColor)

            // Needle tip glow
            drawCircle(
                color = accuracyColor.copy(alpha = 0.3f),
                radius = 10f,
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = accuracyColor,
                radius = 5f,
                center = Offset(tipX, tipY)
            )

            // Center pivot — layered for depth
            drawCircle(color = Color(0xFF2D2B55), radius = 14f, center = Offset(centerX, bottomY))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accuracyColor, accuracyColor.copy(alpha = 0.6f)),
                    center = Offset(centerX - 2f, bottomY - 2f),
                    radius = 10f
                ),
                radius = 9f,
                center = Offset(centerX, bottomY)
            )
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 3f, center = Offset(centerX - 2f, bottomY - 3f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // — Cents readout — clearly below the gauge, no overlap —
        val displayCents = centsOffset.toInt()
        val sign = if (displayCents > 0) "+" else ""

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$sign$displayCents",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = accuracyColor,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "ct",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextSubtle,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

private fun DrawScope.drawTicks(
    cx: Float, cy: Float, radius: Float, arcThickness: Float,
    startAngle: Float, sweepAngle: Float
) {
    val numTicks = 21
    for (i in 0 until numTicks) {
        val t = i.toFloat() / (numTicks - 1)
        val angleDeg = startAngle + sweepAngle * t
        val angleRad = angleDeg * PI.toFloat() / 180f
        val isMajor = i % 5 == 0
        val isCenter = i == numTicks / 2

        val inner = radius - arcThickness * 0.6f - if (isMajor) 6f else 0f
        val outer = radius + arcThickness * 0.6f + if (isMajor) 4f else 0f

        drawLine(
            color = when {
                isCenter -> Color(0xFF2D2B55)
                isMajor -> Color(0xFFA09DC0)
                else -> Color(0xFFD0CEE0)
            },
            start = Offset(cx + inner * cos(angleRad), cy + inner * sin(angleRad)),
            end = Offset(cx + outer * cos(angleRad), cy + outer * sin(angleRad)),
            strokeWidth = when {
                isCenter -> 3f
                isMajor -> 2.5f
                else -> 1f
            },
            cap = StrokeCap.Round
        )
    }
}
