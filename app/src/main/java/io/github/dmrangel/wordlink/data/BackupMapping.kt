package io.github.dmrangel.wordlink.data

import io.github.dmrangel.wordlink.backup.Backup
import io.github.dmrangel.wordlink.game.Language
import java.time.LocalDate

// Converts between the Android profile and the backup code shared with the web app.

fun Profile.toBackup(): Backup = Backup(
    coins = coins,
    jar = jar,
    wordsFound = wordsFound,
    bonusFound = bonusFound,
    streak = streak,
    lastDaily = lastDaily?.toEpochDay()?.toInt(),
    levels = levels.mapKeys { it.key.code },
    dailyDone = dailyDone.mapNotNull { (language, date) -> date?.let { language.code to it.toEpochDay().toInt() } }.toMap(),
)

fun Backup.toProfile(): Profile = Profile(
    coins = coins,
    jar = jar,
    levels = Language.entries.associateWith { levels[it.code] ?: 1 },
    wordsFound = wordsFound,
    bonusFound = bonusFound,
    streak = streak,
    lastDaily = lastDaily?.let { LocalDate.ofEpochDay(it.toLong()) },
    dailyDone = Language.entries.associateWith { language -> dailyDone[language.code]?.let { LocalDate.ofEpochDay(it.toLong()) } },
)
