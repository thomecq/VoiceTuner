package com.voicetuner.game

import com.voicetuner.model.GameConfig
import com.voicetuner.model.GameDifficulty
import com.voicetuner.model.GameRound
import com.voicetuner.model.GameType
import com.voicetuner.model.Note
import com.voicetuner.model.OctaveRange
import kotlin.random.Random

object NoteGenerator {

    fun generateRounds(config: GameConfig): List<GameRound> {
        val rounds = mutableListOf<GameRound>()
        var previousMidi: Int? = null

        for (i in 0 until config.roundCount) {
            if (config.type == GameType.SINGLE_NOTES) {
                val note = generateSingleNote(config.difficulty, config.octaveRange, previousMidi)
                previousMidi = note.midiNumber
                rounds.add(GameRound(index = i, targetNotes = listOf(note), isChord = false))
            } else {
                val chord = generateChord(config.octaveRange)
                rounds.add(GameRound(index = i, targetNotes = chord, isChord = true))
            }
        }
        return rounds
    }

    private fun generateSingleNote(difficulty: GameDifficulty, range: OctaveRange, previousMidi: Int?): Note {
        val candidates = (range.minMidi..range.maxMidi)
            .map { Note.fromMidi(it) }
            .filter { note ->
                if (difficulty == GameDifficulty.EASY) !note.isBlack else true
            }
            .filter { it.midiNumber != previousMidi }

        return candidates.random()
    }

    private fun generateChord(range: OctaveRange): List<Note> {
        val chordBaseMin = range.minMidi
        val chordBaseMax = range.chordBaseMax.coerceAtMost(range.maxMidi - 7)

        val baseMidi = Random.nextInt(chordBaseMin, chordBaseMax + 1)
        val isTriad = Random.nextBoolean()

        val intervals = if (isTriad) {
            if (Random.nextBoolean()) listOf(0, 4, 7) else listOf(0, 3, 7)
        } else {
            val interval = listOf(3, 4, 5, 7).random()
            listOf(0, interval)
        }

        return intervals.map { Note.fromMidi(baseMidi + it) }
    }
}
