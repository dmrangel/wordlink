package io.github.dmrangel.wordlink.game

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs on the JVM (what Android ships) and on JS (what the web app ships):
 * both must rebuild exactly the levels recorded from the original engine, or
 * the same level number would be a different puzzle on each platform.
 */
class EngineParityTest {
    @Test
    fun levelsMatchRecordedFixture() {
        val generators = Language.entries.associateWith { LevelGenerator(Dictionary.parse(it, readDictionary(it.code))) }
        for (line in LevelFixture) {
            val key = line.substringBefore('|')
            val language = Language.fromCode(key.substringBefore(':'))!!
            val id = key.substringAfter(':')
            val generator = generators.getValue(language)
            val level = if (id.startsWith("d")) generator.daily(id.drop(1).toInt()) else generator.campaign(id.toInt())
            val actual = key + "|" + level.letters + "|" + level.rows + "x" + level.cols + "|" +
                level.placements.joinToString(",") { "${it.word}@${it.row}.${it.col}${if (it.across) "a" else "d"}" }
            assertEquals(line, actual)
        }
    }

    @Test
    fun dailyKeysUseIsoDates() {
        val generator = LevelGenerator(Dictionary.parse(Language.English, readDictionary("en")))
        assertEquals("en:d2026-11-08", generator.daily(20_765).id.key)
    }

    @Test
    fun isoDateMatchesJavaTime() {
        assertEquals("1970-01-01", isoDate(0))
        assertEquals("1969-12-31", isoDate(-1))
        assertEquals("2000-02-29", isoDate(11_016))
        assertEquals("2026-11-08", isoDate(20_765))
        assertEquals("2100-03-01", isoDate(47_541))
    }
}
