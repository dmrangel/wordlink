package io.github.dmrangel.wordlink.data

import android.content.Context
import androidx.core.content.edit
import io.github.dmrangel.wordlink.game.Cell
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.game.LevelId
import io.github.dmrangel.wordlink.ui.theme.CellShape
import io.github.dmrangel.wordlink.ui.theme.Customization
import io.github.dmrangel.wordlink.ui.theme.ThemeMode
import java.time.LocalDate

data class Settings(
    val language: Language,
    val theme: ThemeMode,
    val sound: Boolean,
    val haptics: Boolean,
    val highRefreshRate: Boolean,
    val custom: Customization = Customization(),
)

data class Profile(
    val coins: Int,
    /** Bonus words collected towards the next jar reward. */
    val jar: Int,
    val levels: Map<Language, Int>,
    val wordsFound: Int,
    val bonusFound: Int,
    val streak: Int,
    val lastDaily: LocalDate?,
    val dailyDone: Map<Language, LocalDate?>,
) {
    fun level(language: Language): Int = levels[language] ?: 1
    fun dailySolved(language: Language, today: LocalDate): Boolean = dailyDone[language] == today
}

/** Words and cells already uncovered in a level that is still in progress. */
data class SavedProgress(val found: Set<String>, val bonus: Set<String>, val hinted: Set<Cell>) {
    companion object {
        val Empty = SavedProgress(emptySet(), emptySet(), emptySet())
    }
}

/** Everything is small key/value data, so plain SharedPreferences is plenty. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("wordlink", Context.MODE_PRIVATE)

    fun settings(): Settings = Settings(
        language = Language.fromCode(prefs.getString("language", null)) ?: defaultLanguage(),
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(ThemeMode.Dark),
        sound = prefs.getBoolean("sound", true),
        haptics = prefs.getBoolean("haptics", true),
        highRefreshRate = prefs.getBoolean("highRefreshRate", true),
        custom = customization(),
    )

    private fun customization(): Customization {
        val defaults = Customization()
        return Customization(
            accent = if (prefs.contains("accent")) prefs.getInt("accent", 0) else null,
            themeBase = prefs.getInt("themeBase", defaults.themeBase),
            background = prefs.getString("background", null),
            blur = prefs.getFloat("blur", defaults.blur),
            dim = prefs.getFloat("dim", defaults.dim),
            panelOpacity = prefs.getFloat("panelOpacity", defaults.panelOpacity),
            cellShape = runCatching { CellShape.valueOf(prefs.getString("cellShape", null)!!) }.getOrDefault(defaults.cellShape),
            wheelScale = prefs.getFloat("wheelScale", defaults.wheelScale),
            leftHanded = prefs.getBoolean("leftHanded", defaults.leftHanded),
            reduceMotion = prefs.getBoolean("reduceMotion", defaults.reduceMotion),
        )
    }

    fun save(settings: Settings) = prefs.edit {
        putString("language", settings.language.code)
        putString("theme", settings.theme.name)
        putBoolean("sound", settings.sound)
        putBoolean("haptics", settings.haptics)
        putBoolean("highRefreshRate", settings.highRefreshRate)
        val custom = settings.custom
        if (custom.accent != null) putInt("accent", custom.accent) else remove("accent")
        putInt("themeBase", custom.themeBase)
        putString("background", custom.background)
        putFloat("blur", custom.blur)
        putFloat("dim", custom.dim)
        putFloat("panelOpacity", custom.panelOpacity)
        putString("cellShape", custom.cellShape.name)
        putFloat("wheelScale", custom.wheelScale)
        putBoolean("leftHanded", custom.leftHanded)
        putBoolean("reduceMotion", custom.reduceMotion)
    }

    fun profile(): Profile = Profile(
        coins = prefs.getInt("coins", STARTING_COINS),
        jar = prefs.getInt("jar", 0),
        levels = Language.entries.associateWith { prefs.getInt("level_${it.code}", 1) },
        wordsFound = prefs.getInt("wordsFound", 0),
        bonusFound = prefs.getInt("bonusFound", 0),
        streak = prefs.getInt("streak", 0),
        lastDaily = prefs.getString("lastDaily", null)?.let(LocalDate::parse),
        dailyDone = Language.entries.associateWith { lang -> prefs.getString("daily_${lang.code}", null)?.let(LocalDate::parse) },
    )

    fun save(profile: Profile) = prefs.edit {
        putInt("coins", profile.coins)
        putInt("jar", profile.jar)
        for ((language, level) in profile.levels) putInt("level_${language.code}", level)
        putInt("wordsFound", profile.wordsFound)
        putInt("bonusFound", profile.bonusFound)
        putInt("streak", profile.streak)
        putString("lastDaily", profile.lastDaily?.toString())
        for ((language, date) in profile.dailyDone) putString("daily_${language.code}", date?.toString())
    }

    fun progress(id: LevelId): SavedProgress {
        val raw = prefs.getString("progress_${id.key}", null) ?: return SavedProgress.Empty
        val parts = raw.split('|')
        fun words(i: Int) = parts.getOrNull(i)?.split(',')?.filter { it.isNotEmpty() }?.toSet().orEmpty()
        val hinted = parts.getOrNull(2)?.split(',')?.mapNotNull { it.toIntOrNull()?.let(::Cell) }?.toSet().orEmpty()
        return SavedProgress(words(0), words(1), hinted)
    }

    fun save(id: LevelId, progress: SavedProgress) = prefs.edit {
        putString(
            "progress_${id.key}",
            listOf(
                progress.found.joinToString(","),
                progress.bonus.joinToString(","),
                progress.hinted.joinToString(",") { it.packed.toString() },
            ).joinToString("|"),
        )
    }

    fun clear(id: LevelId) = prefs.edit { remove("progress_${id.key}") }

    /** Drops unfinished daily puzzles from previous days. */
    fun clearStaleDailies(today: LevelId.Daily) {
        val prefix = "progress_${today.language.code}:d"
        val stale = prefs.all.keys.filter { it.startsWith(prefix) && it != "progress_${today.key}" }
        if (stale.isNotEmpty()) prefs.edit { stale.forEach(::remove) }
    }

    private fun defaultLanguage(): Language =
        if (java.util.Locale.getDefault().language == "pt") Language.Portuguese else Language.English

    companion object {
        const val STARTING_COINS = 150
    }
}
