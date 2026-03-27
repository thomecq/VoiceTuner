package com.voicetuner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicetuner.model.Note
import com.voicetuner.ui.theme.KeyPressedBlack
import com.voicetuner.ui.theme.KeyPressedWhite
import com.voicetuner.ui.theme.PianoBlack
import com.voicetuner.ui.theme.PianoBlackLight
import com.voicetuner.ui.theme.PianoWhite
import com.voicetuner.ui.theme.PianoWhiteShadow

private val WHITE_KEY_WIDTH = 52.dp
private val WHITE_KEY_HEIGHT = 200.dp
private val BLACK_KEY_WIDTH = 32.dp
private val BLACK_KEY_HEIGHT = 120.dp

@Composable
fun PianoKeyboard(
    notes: List<Note>,
    pressedKeys: Set<Int>,
    onKeyPress: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val whiteNotes = notes.filter { !it.isBlack }
    val blackNotes = notes.filter { it.isBlack }
    val totalWidth = WHITE_KEY_WIDTH * whiteNotes.size
    val textMeasurer = rememberTextMeasurer()
    val scrollState = rememberScrollState()

    Canvas(
        modifier = modifier
            .horizontalScroll(scrollState)
            .width(totalWidth)
            .height(WHITE_KEY_HEIGHT)
            .pointerInput(notes) {
                detectTapGestures { offset ->
                    val whiteKeyWidthPx = WHITE_KEY_WIDTH.toPx()
                    val blackKeyWidthPx = BLACK_KEY_WIDTH.toPx()
                    val blackKeyHeightPx = BLACK_KEY_HEIGHT.toPx()

                    // Check black keys first (they're on top)
                    if (offset.y < blackKeyHeightPx) {
                        for ((index, whiteNote) in whiteNotes.withIndex()) {
                            val blackNote = findBlackKeyAfter(whiteNote, blackNotes)
                            if (blackNote != null) {
                                val blackX = (index + 1) * whiteKeyWidthPx - blackKeyWidthPx / 2
                                if (offset.x >= blackX && offset.x <= blackX + blackKeyWidthPx) {
                                    onKeyPress(blackNote)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }

                    // Check white keys
                    val whiteIndex = (offset.x / whiteKeyWidthPx).toInt()
                    if (whiteIndex in whiteNotes.indices) {
                        onKeyPress(whiteNotes[whiteIndex])
                    }
                }
            }
    ) {
        val whiteKeyWidthPx = WHITE_KEY_WIDTH.toPx()
        val whiteKeyHeightPx = WHITE_KEY_HEIGHT.toPx()
        val blackKeyWidthPx = BLACK_KEY_WIDTH.toPx()
        val blackKeyHeightPx = BLACK_KEY_HEIGHT.toPx()
        val cornerR = 6f

        // Draw white keys
        for ((index, note) in whiteNotes.withIndex()) {
            val x = index * whiteKeyWidthPx
            val isPressed = note.midiNumber in pressedKeys
            val gap = 1.5f

            // Key shadow (bottom)
            if (!isPressed) {
                drawRoundRect(
                    color = PianoWhiteShadow,
                    topLeft = Offset(x + gap, 3f),
                    size = Size(whiteKeyWidthPx - gap * 2, whiteKeyHeightPx - 3f),
                    cornerRadius = CornerRadius(cornerR)
                )
            }

            // Key body with gradient
            val keyColor = if (isPressed) KeyPressedWhite else PianoWhite
            val keyBrush = if (isPressed) {
                Brush.verticalGradient(
                    colors = listOf(KeyPressedWhite, KeyPressedWhite.copy(alpha = 0.8f))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))
                )
            }
            drawRoundRect(
                brush = keyBrush,
                topLeft = Offset(x + gap, if (isPressed) 2f else 0f),
                size = Size(whiteKeyWidthPx - gap * 2, whiteKeyHeightPx - if (isPressed) 5f else 6f),
                cornerRadius = CornerRadius(cornerR)
            )

            // Subtle border
            drawRoundRect(
                color = Color(0x20000000),
                topLeft = Offset(x + gap, 0f),
                size = Size(whiteKeyWidthPx - gap * 2, whiteKeyHeightPx - 6f),
                cornerRadius = CornerRadius(cornerR),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
            )

            // Note label at bottom
            val label = note.name + note.octave.toString()
            val textResult = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = if (isPressed) Color(0xFF6C63FF) else Color(0xFF9896B8),
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x + (whiteKeyWidthPx - textResult.size.width) / 2,
                    whiteKeyHeightPx - textResult.size.height - 16f
                )
            )
        }

        // Draw black keys
        for ((index, whiteNote) in whiteNotes.withIndex()) {
            val blackNote = findBlackKeyAfter(whiteNote, blackNotes)
            if (blackNote != null) {
                val x = (index + 1) * whiteKeyWidthPx - blackKeyWidthPx / 2
                val isPressed = blackNote.midiNumber in pressedKeys

                // Shadow
                if (!isPressed) {
                    drawRoundRect(
                        color = Color(0x40000000),
                        topLeft = Offset(x - 1f, 3f),
                        size = Size(blackKeyWidthPx + 2f, blackKeyHeightPx + 3f),
                        cornerRadius = CornerRadius(4f)
                    )
                }

                // Key body with gradient
                val keyBrush = if (isPressed) {
                    Brush.verticalGradient(
                        colors = listOf(KeyPressedBlack, KeyPressedBlack.copy(alpha = 0.8f))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(PianoBlackLight, PianoBlack)
                    )
                }
                drawRoundRect(
                    brush = keyBrush,
                    topLeft = Offset(x, if (isPressed) 2f else 0f),
                    size = Size(blackKeyWidthPx, blackKeyHeightPx - if (isPressed) 2f else 0f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Shine highlight on top of black key
                if (!isPressed) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x20FFFFFF), Color.Transparent),
                            endY = blackKeyHeightPx * 0.3f
                        ),
                        topLeft = Offset(x + 2f, 0f),
                        size = Size(blackKeyWidthPx - 4f, blackKeyHeightPx * 0.4f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

private fun findBlackKeyAfter(whiteNote: Note, blackNotes: List<Note>): Note? {
    val notesThatHaveBlackAfter = setOf("C", "D", "F", "G", "A")
    if (whiteNote.name !in notesThatHaveBlackAfter) return null

    return blackNotes.find {
        it.octave == whiteNote.octave && it.midiNumber == whiteNote.midiNumber + 1
    }
}
