package io.github.dmrangel.wordlink.game

import java.time.LocalDate

sealed interface LevelId {
    val language: Language
    val key: String

    data class Campaign(override val language: Language, val number: Int) : LevelId {
        override val key: String get() = "${language.code}:$number"
    }

    data class Daily(override val language: Language, val date: LocalDate) : LevelId {
        override val key: String get() = "${language.code}:d$date"
    }
}

class Level(
    val id: LevelId,
    /** Wheel letters in their initial order. */
    val letters: String,
    val placements: List<Placement>,
    val rows: Int,
    val cols: Int,
) {
    val words: List<String> = placements.map { it.word }
    val letterAt: Map<Cell, Char> = buildMap {
        for (p in placements) p.cells.forEachIndexed { i, cell -> put(cell, p.word[i]) }
    }
    val cellsOf: Map<String, List<Cell>> = placements.associate { it.word to it.cells }
}
