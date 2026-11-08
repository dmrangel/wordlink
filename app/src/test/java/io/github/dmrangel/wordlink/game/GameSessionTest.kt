package io.github.dmrangel.wordlink.game

import io.github.dmrangel.wordlink.data.SavedProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    private val dictionary = Dictionary.parse(
        Language.Portuguese,
        "@common\nmesa\nmae\nmas\nsem\nmes\n@valid 3\namamaemasmessem\n@valid 4\namasmesa\n@display\nmae mãe\nmes mês\n",
    )

    /**
     * Portuguese level 3: MAS runs down column 3 and every one of its letters
     * is also part of MAE, MESA or SEM.
     *
     *   . M . M A E
     *   M E S A . .
     *   . S . S E M
     */
    private val level = Level(
        id = LevelId.Campaign(Language.Portuguese, 3),
        letters = "mase",
        placements = listOf(
            Placement("mas", 0, 3, across = false),
            Placement("mesa", 1, 0, across = true),
            Placement("mae", 0, 3, across = true),
            Placement("sem", 2, 3, across = true),
            Placement("mes", 0, 1, across = false),
        ),
        rows = 3,
        cols = 6,
    )

    private class Recorder : GameSession.Listener {
        var solved = 0
        var coins = 100
        override fun onProgress(session: GameSession) = Unit
        override fun onWordFound(bonus: Boolean) = Unit
        override fun trySpend(coins: Int): Boolean = (this.coins >= coins).also { if (it) this.coins -= coins }
        override fun onSolved(session: GameSession) {
            solved++
        }
    }

    @Test
    fun wordCoveredByCrossingsCountsAsFound() {
        val recorder = Recorder()
        val session = GameSession(level, dictionary, SavedProgress.Empty, recorder)
        for (word in listOf("mesa", "mae", "sem", "mes")) assertEquals(WordResult.Found, session.submit(word))
        assertTrue("mas" in session.found)
        assertTrue(session.isSolved)
        assertEquals(1, recorder.solved)
    }

    @Test
    fun restoredProgressAbsorbsUncoveredWords() {
        val saved = SavedProgress(setOf("mesa", "mae", "sem", "mes"), emptySet(), emptySet())
        val session = GameSession(level, dictionary, saved, Recorder())
        assertTrue(session.isSolved)
    }

    @Test
    fun bonusAndInvalidWords() {
        val session = GameSession(level, dictionary, SavedProgress.Empty, Recorder())
        assertEquals(WordResult.Bonus, session.submit("ama"))
        assertEquals(WordResult.BonusRepeat, session.submit("ama"))
        assertEquals(WordResult.Invalid, session.submit("xyz"))
        assertEquals(WordResult.Found, session.submit("mesa"))
        assertEquals(WordResult.Repeat, session.submit("mesa"))
        assertEquals("mês", session.display("mes"))
        assertFalse(session.isSolved)
    }

    @Test
    fun hintsCompleteWordsAndCostCoins() {
        val recorder = Recorder()
        val session = GameSession(level, dictionary, SavedProgress.Empty, recorder)
        repeat(4) { assertTrue(session.hint(25)) }
        assertEquals(0, recorder.coins)
        assertFalse(session.hint(25))
        assertEquals(4, session.hinted.size)
    }
}
