package io.github.dmrangel.wordlink.game

import kotlin.random.Random

/** A grid position packed into one Int so cell sets stay cheap. */
@JvmInline
value class Cell(val packed: Int) {
    constructor(row: Int, col: Int) : this(row * STRIDE + col)

    val row: Int get() = packed / STRIDE
    val col: Int get() = packed % STRIDE

    companion object {
        const val STRIDE = 64
    }
}

data class Placement(val word: String, val row: Int, val col: Int, val across: Boolean) {
    val cells: List<Cell>
        get() = List(word.length) { i -> if (across) Cell(row, col + i) else Cell(row + i, col) }
}

class Layout(val placements: List<Placement>, val rows: Int, val cols: Int)

/**
 * Builds Wordscapes-style crosswords: every word crosses at least one other
 * word, words never touch side by side, and the grid stays within a box.
 * Several randomized attempts are made and the densest layout wins.
 */
object CrosswordBuilder {
    private const val SIZE = 48
    private const val ORIGIN = SIZE / 2
    private const val EMPTY = ' '
    private const val ACROSS: Byte = 1
    private const val DOWN: Byte = 2

    fun build(
        words: List<String>,
        maxWords: Int,
        maxRows: Int,
        maxCols: Int,
        random: Random,
        attempts: Int = 48,
    ): Layout? {
        if (words.isEmpty()) return null
        val first = words.first()
        val others = words.drop(1)
        var best: Attempt? = null
        repeat(attempts) { index ->
            val order = others.shuffled(random).sortedByDescending { it.length }
            val attempt = Attempt(maxRows, maxCols)
            if (!attempt.placeFirst(first, across = index % 4 != 3)) return@repeat
            val skipped = ArrayList<String>()
            for (word in order) {
                if (attempt.count >= maxWords) break
                if (!attempt.placeBest(word, random)) skipped += word
            }
            // A later word can open up spots for one that failed earlier.
            for (word in skipped) {
                if (attempt.count >= maxWords) break
                attempt.placeBest(word, random)
            }
            if (attempt.score > (best?.score ?: Double.NEGATIVE_INFINITY)) best = attempt
        }
        return best?.toLayout()
    }

    private class Attempt(val maxRows: Int, val maxCols: Int) {
        val letters = Array(SIZE) { CharArray(SIZE) { EMPTY } }
        val directions = Array(SIZE) { ByteArray(SIZE) }
        val placements = ArrayList<Placement>()
        var minRow = ORIGIN
        var maxRow = ORIGIN
        var minCol = ORIGIN
        var maxCol = ORIGIN
        var crossings = 0

        val count: Int get() = placements.size
        val score: Double
            get() {
                val area = (maxRow - minRow + 1) * (maxCol - minCol + 1)
                return count * 1000.0 + crossings * 25.0 - area
            }

        fun placeFirst(word: String, across: Boolean): Boolean {
            if (across && word.length > maxCols || !across && word.length > maxRows) return false
            put(word, ORIGIN, ORIGIN, across)
            return true
        }

        fun placeBest(word: String, random: Random): Boolean {
            var bestScore = Double.NEGATIVE_INFINITY
            var bestRow = 0
            var bestCol = 0
            var bestAcross = true
            for (p in placements) {
                for ((cellIndex, cell) in p.cells.withIndex()) {
                    val letter = p.word[cellIndex]
                    for (i in word.indices) {
                        if (word[i] != letter) continue
                        val across = !p.across
                        val row = if (across) cell.row else cell.row - i
                        val col = if (across) cell.col - i else cell.col
                        val crosses = crossingsFor(word, row, col, across)
                        if (crosses <= 0) continue
                        val score = scoreFor(word, row, col, across, crosses) + random.nextDouble() * 4.0
                        if (score > bestScore) {
                            bestScore = score
                            bestRow = row
                            bestCol = col
                            bestAcross = across
                        }
                    }
                }
            }
            if (bestScore == Double.NEGATIVE_INFINITY) return false
            put(word, bestRow, bestCol, bestAcross)
            return true
        }

        private fun occupied(row: Int, col: Int): Boolean =
            row in 0 until SIZE && col in 0 until SIZE && letters[row][col] != EMPTY

        /** Number of crossings if the word fits at this spot, or -1 if it doesn't. */
        private fun crossingsFor(word: String, row: Int, col: Int, across: Boolean): Int {
            val dr = if (across) 0 else 1
            val dc = if (across) 1 else 0
            val endRow = row + dr * (word.length - 1)
            val endCol = col + dc * (word.length - 1)
            if (row < 1 || col < 1 || endRow >= SIZE - 1 || endCol >= SIZE - 1) return -1
            if (maxOf(maxRow, endRow) - minOf(minRow, row) + 1 > maxRows) return -1
            if (maxOf(maxCol, endCol) - minOf(minCol, col) + 1 > maxCols) return -1
            if (occupied(row - dr, col - dc) || occupied(endRow + dr, endCol + dc)) return -1

            val direction = if (across) ACROSS else DOWN
            var crosses = 0
            for (i in word.indices) {
                val r = row + dr * i
                val c = col + dc * i
                val existing = letters[r][c]
                if (existing != EMPTY) {
                    if (existing != word[i]) return -1
                    if (directions[r][c].toInt() and direction.toInt() != 0) return -1
                    crosses++
                } else if (across) {
                    if (occupied(r - 1, c) || occupied(r + 1, c)) return -1
                } else {
                    if (occupied(r, c - 1) || occupied(r, c + 1)) return -1
                }
            }
            return if (crosses == word.length) -1 else crosses
        }

        private fun scoreFor(word: String, row: Int, col: Int, across: Boolean, crosses: Int): Double {
            val endRow = if (across) row else row + word.length - 1
            val endCol = if (across) col + word.length - 1 else col
            val rows = maxOf(maxRow, endRow) - minOf(minRow, row) + 1
            val cols = maxOf(maxCol, endCol) - minOf(minCol, col) + 1
            val oldArea = (maxRow - minRow + 1) * (maxCol - minCol + 1)
            // Favor crossings and compact grids that are a little wider than tall,
            // since the letter wheel takes the bottom half of a portrait screen.
            return crosses * 12.0 - (rows * cols - oldArea) * 0.7 - maxOf(0, rows - cols) * 3.0
        }

        private fun put(word: String, row: Int, col: Int, across: Boolean) {
            val dr = if (across) 0 else 1
            val dc = if (across) 1 else 0
            val direction = if (across) ACROSS else DOWN
            for (i in word.indices) {
                val r = row + dr * i
                val c = col + dc * i
                if (letters[r][c] != EMPTY) crossings++
                letters[r][c] = word[i]
                directions[r][c] = (directions[r][c].toInt() or direction.toInt()).toByte()
            }
            placements += Placement(word, row, col, across)
            minRow = minOf(minRow, row)
            minCol = minOf(minCol, col)
            maxRow = maxOf(maxRow, row + dr * (word.length - 1))
            maxCol = maxOf(maxCol, col + dc * (word.length - 1))
        }

        fun toLayout(): Layout = Layout(
            placements = placements.map { it.copy(row = it.row - minRow, col = it.col - minCol) },
            rows = maxRow - minRow + 1,
            cols = maxCol - minCol + 1,
        )
    }
}
