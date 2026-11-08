@file:OptIn(ExperimentalJsExport::class)

package io.github.dmrangel.wordlink.web

import io.github.dmrangel.wordlink.backup.Backup
import io.github.dmrangel.wordlink.backup.BackupCode
import io.github.dmrangel.wordlink.game.Dictionary
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.game.Level
import io.github.dmrangel.wordlink.game.LevelGenerator

// The small surface the web app (web/) calls; everything else stays internal.

@JsExport
class WebPlacement(val word: String, val row: Int, val col: Int, val across: Boolean)

@JsExport
class WebLevel(
    /** Same key Android uses for saved progress: "en:12" or "en:d2026-11-08". */
    val key: String,
    val letters: String,
    val rows: Int,
    val cols: Int,
    val placements: Array<WebPlacement>,
)

@JsExport
class Engine(languageCode: String, dictionaryText: String) {
    private val generator = LevelGenerator(
        Dictionary.parse(Language.fromCode(languageCode) ?: throw IllegalArgumentException(languageCode), dictionaryText),
    )

    fun campaign(number: Int): WebLevel = generator.campaign(number).toWeb()

    fun daily(epochDay: Int): WebLevel = generator.daily(epochDay).toWeb()

    fun isValid(word: String): Boolean = generator.dictionary.isValid(word)

    fun display(word: String): String = generator.dictionary.display(word)

    private fun Level.toWeb() = WebLevel(
        id.key,
        letters,
        rows,
        cols,
        placements.map { WebPlacement(it.word, it.row, it.col, it.across) }.toTypedArray(),
    )
}

/** Mirrors [Backup]; maps become parallel arrays so plain JS can build it. */
@JsExport
class WebBackup(
    val coins: Int,
    val jar: Int,
    val wordsFound: Int,
    val bonusFound: Int,
    val streak: Int,
    /** Epoch day, or -1 when no daily was ever solved. */
    val lastDaily: Int,
    val levelLanguages: Array<String>,
    val levels: IntArray,
    val dailyLanguages: Array<String>,
    val dailyDays: IntArray,
)

@JsExport
fun encodeBackup(backup: WebBackup): String = BackupCode.encode(
    Backup(
        coins = backup.coins,
        jar = backup.jar,
        wordsFound = backup.wordsFound,
        bonusFound = backup.bonusFound,
        streak = backup.streak,
        lastDaily = backup.lastDaily.takeIf { it >= 0 },
        levels = backup.levelLanguages.indices.associate { backup.levelLanguages[it] to backup.levels[it] },
        dailyDone = backup.dailyLanguages.indices.associate { backup.dailyLanguages[it] to backup.dailyDays[it] },
    ),
)

@JsExport
fun decodeBackup(text: String): WebBackup? {
    val backup = BackupCode.decode(text) ?: return null
    return WebBackup(
        coins = backup.coins,
        jar = backup.jar,
        wordsFound = backup.wordsFound,
        bonusFound = backup.bonusFound,
        streak = backup.streak,
        lastDaily = backup.lastDaily ?: -1,
        levelLanguages = backup.levels.keys.toTypedArray(),
        levels = backup.levels.values.toIntArray(),
        dailyLanguages = backup.dailyDone.keys.toTypedArray(),
        dailyDays = backup.dailyDone.values.toIntArray(),
    )
}
