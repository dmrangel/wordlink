package io.github.dmrangel.wordlink.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class LevelGeneratorTest {
    private val dictionaries = Language.entries.associateWith { language ->
        Dictionary.parse(language, File("src/main/assets/dict/${language.code}.txt").readText())
    }

    @Test
    fun dictionariesLoad() {
        for ((language, dictionary) in dictionaries) {
            assertTrue("$language common", dictionary.common.size > 15_000)
            assertTrue("$language valid", dictionary.validCount > 30_000)
            assertTrue(dictionary.common.all { dictionary.isValid(it) })
        }
        val en = dictionaries.getValue(Language.English)
        assertTrue(en.isValid("house"))
        assertTrue(en.isValid("houses"))
        assertFalse(en.isValid("hosue"))
        assertFalse(en.isValid("shit"))

        val pt = dictionaries.getValue(Language.Portuguese)
        assertTrue(pt.isValid("casa"))
        // Portuguese only takes headwords: no plurals, no conjugated verbs.
        for (word in listOf("casas", "amigos", "foi", "falou", "estou")) assertFalse(word, pt.isValid(word))
        for (word in listOf("amiga", "bonita", "falar", "mais", "seis", "onibus")) assertTrue(word, pt.isValid(word))
        assertTrue(pt.isValid("acao"))
        assertEquals("ação", pt.display("acao"))
        assertEquals("coração", pt.display("coracao"))
        assertEquals("casa", pt.display("casa"))
        assertFalse(pt.isValid("porra"))
    }

    @Test
    fun subAnagramsRespectLetterCounts() {
        val en = dictionaries.getValue(Language.English)
        val words = en.subAnagrams("letters", 20_000, 3)
        assertTrue("letter" in words)
        assertTrue(words.none { word -> word.groupingBy { it }.eachCount().any { (ch, n) -> n > "letters".count { it == ch } } })
    }

    @Test
    fun campaignLevelsAreValidCrosswords() {
        for ((language, dictionary) in dictionaries) {
            val generator = LevelGenerator(dictionary)
            for (number in 1..400) {
                val level = generator.campaign(number)
                val difficulty = Difficulty.forCampaign(number)
                assertEquals("$language #$number letters", difficulty.letters, level.letters.length)
                assertTrue("$language #$number too few words", level.words.size >= minOf(difficulty.minWords, 3))
                assertTrue("$language #$number too many words", level.words.size <= difficulty.maxWords)
                assertTrue(level.rows <= difficulty.maxRows && level.cols <= difficulty.maxCols)
                checkLevel(level, dictionary)
            }
        }
    }

    @Test
    fun dailyLevelsAreValid() {
        for ((_, dictionary) in dictionaries) {
            val generator = LevelGenerator(dictionary)
            var date = LocalDate.of(2026, 1, 1)
            repeat(120) {
                checkLevel(generator.daily(date), dictionary)
                date = date.plusDays(1)
            }
        }
    }

    @Test
    fun generationIsDeterministic() {
        val dictionary = dictionaries.getValue(Language.Portuguese)
        for (number in listOf(1, 12, 77, 500)) {
            val a = LevelGenerator(dictionary).campaign(number)
            val b = LevelGenerator(dictionary).campaign(number)
            assertEquals(a.letters, b.letters)
            assertEquals(a.placements, b.placements)
        }
    }

    @Test
    fun levelsNeverRepeatTheSameLetters() {
        for ((language, dictionary) in dictionaries) {
            val generator = LevelGenerator(dictionary)
            val seen = HashMap<String, Int>()
            for (number in 1..400) {
                val letters = generator.campaign(number).letters.toList().sorted().joinToString("")
                val previous = seen.put(letters, number)
                assertTrue("$language level $number repeats level $previous ($letters)", previous == null)
            }
        }
    }

    private fun checkLevel(level: Level, dictionary: Dictionary) {
        val id = level.id.key
        assertEquals("$id duplicate words", level.words.size, level.words.toSet().size)
        assertTrue("$id base word must use every letter", level.words.any { it.length == level.letters.length })

        val grid = HashMap<Cell, Char>()
        val directions = HashMap<Cell, Int>()
        for (p in level.placements) {
            assertTrue("$id ${p.word} invalid", dictionary.isValid(p.word))
            assertTrue("$id ${p.word} not spellable", fits(p.word, level.letters))
            for ((i, cell) in p.cells.withIndex()) {
                assertTrue(cell.row in 0 until level.rows && cell.col in 0 until level.cols)
                val existing = grid.put(cell, p.word[i])
                assertTrue("$id letter clash at $cell", existing == null || existing == p.word[i])
                val bit = if (p.across) 1 else 2
                val previous = directions[cell] ?: 0
                assertEquals("$id parallel overlap", 0, previous and bit)
                directions[cell] = previous or bit
            }
        }
        // Every maximal run of 2+ letters in the grid must be one of the placed words.
        val words = level.words.toSet()
        for (across in listOf(true, false)) {
            for (line in 0 until if (across) level.rows else level.cols) {
                val run = StringBuilder()
                for (i in 0..(if (across) level.cols else level.rows)) {
                    val cell = if (across) Cell(line, i) else Cell(i, line)
                    val ch = if (i < (if (across) level.cols else level.rows)) grid[cell] else null
                    if (ch != null) {
                        run.append(ch)
                    } else {
                        if (run.length > 1) assertTrue("$id stray word $run", run.toString() in words)
                        run.clear()
                    }
                }
            }
        }
        assertNotNull(level.letterAt)
    }

    private fun fits(word: String, letters: String): Boolean {
        val pool = letters.toMutableList()
        return word.all { pool.remove(it) }
    }
}
