package io.github.dmrangel.wordlink.game

sealed interface LevelId {
    val language: Language
    val key: String

    data class Campaign(override val language: Language, val number: Int) : LevelId {
        override val key: String get() = "${language.code}:$number"
    }

    /** [epochDay] counts days since 1970-01-01, like java.time.LocalDate.toEpochDay(). */
    data class Daily(override val language: Language, val epochDay: Int) : LevelId {
        override val key: String get() = "${language.code}:d${isoDate(epochDay)}"
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

/** "yyyy-MM-dd" for a day count since 1970-01-01 (proleptic Gregorian, as java.time does). */
fun isoDate(epochDay: Int): String {
    // Howard Hinnant's civil_from_days.
    val z = epochDay + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yoe + era * 400 + if (month <= 2) 1 else 0
    return year.toString().padStart(4, '0') + "-" + month.toString().padStart(2, '0') + "-" + day.toString().padStart(2, '0')
}
