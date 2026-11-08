package io.github.dmrangel.wordlink.game

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dmrangel.wordlink.data.SavedProgress
import kotlin.random.Random

enum class WordResult { Found, Repeat, Bonus, BonusRepeat, Invalid }

/** What the word preview shows after a submission. [id] makes equal results distinct. */
data class Feedback(val text: String, val result: WordResult, val id: Long)

/** Briefly highlights the cells of a word the player found again. */
data class Pulse(val word: String, val id: Long)

/**
 * State of one puzzle being played. Exposes Compose state so the UI reads it
 * directly; every change is reported to [Listener] for saving and rewards.
 */
@Stable
class GameSession(
    val level: Level,
    private val dictionary: Dictionary,
    saved: SavedProgress,
    private val listener: Listener,
) {
    interface Listener {
        fun onProgress(session: GameSession)
        fun onWordFound(bonus: Boolean)
        fun trySpend(coins: Int): Boolean
        fun onSolved(session: GameSession)
    }

    private val allCells = level.letterAt.keys

    var found by mutableStateOf<Set<String>>(saved.found.filterTo(LinkedHashSet()) { it in level.cellsOf })
        private set
    var bonus by mutableStateOf(saved.bonus)
        private set
    var hinted by mutableStateOf<Set<Cell>>(saved.hinted.filterTo(HashSet()) { it in allCells })
        private set

    /** Wheel order as indices into [Level.letters]; shuffling permutes it. */
    var order by mutableStateOf(level.letters.indices.toList())
        private set
    var feedback by mutableStateOf<Feedback?>(null)
        private set
    var pulse by mutableStateOf<Pulse?>(null)
        private set
    var targeting by mutableStateOf(false)

    /** Delay slot for each newly revealed cell, so a word lights up letter by letter. */
    var revealDelay by mutableStateOf<Map<Cell, Int>>(emptyMap())
        private set

    private var events = mutableIntStateOf(0)

    val solvedCells: Set<Cell> by derivedStateOf {
        found.flatMapTo(HashSet()) { level.cellsOf.getValue(it) }
    }

    val isSolved: Boolean by derivedStateOf { found.size == level.words.size }

    init {
        absorbUncoveredWords()
    }

    fun display(word: String): String = dictionary.display(word)

    fun submit(word: String): WordResult {
        if (word.length < LevelGenerator.MIN_WORD_LENGTH || isSolved) return WordResult.Invalid.also { if (word.length >= 2) show(word, it) }
        val result = when {
            word in found -> WordResult.Repeat.also { pulse = Pulse(word, nextId()) }
            word in level.cellsOf -> {
                revealDelay = level.cellsOf.getValue(word).withIndex().associate { (i, cell) -> cell to i }
                found = found + word
                listener.onWordFound(bonus = false)
                absorbUncoveredWords()
                WordResult.Found
            }
            word in bonus -> WordResult.BonusRepeat
            dictionary.isValid(word) -> {
                bonus = bonus + word
                listener.onWordFound(bonus = true)
                WordResult.Bonus
            }
            else -> WordResult.Invalid
        }
        show(word, result)
        if (result == WordResult.Found || result == WordResult.Bonus) changed()
        return result
    }

    fun shuffle() {
        val current = order
        var next = current
        val random = Random(System.nanoTime())
        while (next == current && current.size > 1) next = current.shuffled(random)
        order = next
    }

    /** Reveals the first hidden letter of a random unfinished word. */
    fun hint(cost: Int): Boolean {
        val visible = solvedCells + hinted
        val word = level.words.filter { it !in found }.randomOrNull() ?: return false
        val cell = level.cellsOf.getValue(word).firstOrNull { it !in visible } ?: return false
        if (!listener.trySpend(cost)) return false
        reveal(cell)
        return true
    }

    /** Reveals a cell the player picked while targeting. */
    fun target(cell: Cell, cost: Int): Boolean {
        targeting = false
        if (cell in solvedCells || cell in hinted || cell !in allCells) return false
        if (!listener.trySpend(cost)) return false
        reveal(cell)
        return true
    }

    private fun reveal(cell: Cell) {
        revealDelay = mapOf(cell to 0)
        hinted = hinted + cell
        absorbUncoveredWords()
        changed()
    }

    /**
     * A word whose letters are all visible counts as found, whether they were
     * uncovered by hints or by the words crossing it (a column made entirely
     * of crossings would otherwise block the level from ever finishing).
     */
    private fun absorbUncoveredWords() {
        val visible = solvedCells + hinted
        val completed = level.words.filter { it !in found && level.cellsOf.getValue(it).all { cell -> cell in visible } }
        if (completed.isNotEmpty()) found = found + completed
    }

    private fun show(word: String, result: WordResult) {
        feedback = Feedback(display(word), result, nextId())
    }

    private fun changed() {
        listener.onProgress(this)
        if (isSolved) listener.onSolved(this)
    }

    private fun nextId(): Long = (++events.intValue).toLong()

    fun snapshot(): SavedProgress = SavedProgress(found, bonus, hinted)
}
