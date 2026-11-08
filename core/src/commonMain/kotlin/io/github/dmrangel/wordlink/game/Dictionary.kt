package io.github.dmrangel.wordlink.game

/**
 * In-memory word list produced by tools/build_dictionaries.py.
 *
 * Kept deliberately compact: valid words live in one fixed-width string per
 * word length and are looked up with a binary search, so a 60k-word language
 * costs a few hundred KB of heap instead of a HashSet with 60k String objects.
 */
class Dictionary private constructor(
    val language: Language,
    /** Answer pool, most common first. The index of a word is its rank. */
    val common: List<String>,
    private val commonMasks: IntArray,
    private val valid: Array<String?>,
    private val displayBlock: String,
    private val displayLines: IntArray,
) {
    val validCount: Int
        get() = valid.indices.sumOf { length -> (valid[length]?.length ?: 0) / length.coerceAtLeast(1) }

    fun isValid(word: String): Boolean {
        val block = valid.getOrNull(word.length) ?: return false
        var low = 0
        var high = block.length / word.length - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cmp = compareRegion(block, mid * word.length, word)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return true
            }
        }
        return false
    }

    /** The accented spelling for Portuguese words ("acao" -> "ação"), or the word itself. */
    fun display(word: String): String {
        var low = 0
        var high = displayLines.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val start = displayLines[mid]
            val space = displayBlock.indexOf(' ', start)
            val cmp = compareKey(displayBlock, start, space, word)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> {
                    val end = displayBlock.indexOf('\n', space).let { if (it < 0) displayBlock.length else it }
                    return displayBlock.substring(space + 1, end)
                }
            }
        }
        return word
    }

    /**
     * Common words (rank below [maxRank]) that can be spelled with [letters],
     * each letter used at most as many times as it appears. Ordered by rank.
     */
    fun subAnagrams(letters: String, maxRank: Int, minLength: Int): List<String> {
        val counts = IntArray(26)
        var mask = 0
        for (ch in letters) {
            counts[ch - 'a']++
            mask = mask or (1 shl (ch - 'a'))
        }
        val scratch = IntArray(26)
        val result = ArrayList<String>()
        val limit = minOf(maxRank, common.size)
        for (i in 0 until limit) {
            val word = common[i]
            if (word.length < minLength || word.length > letters.length) continue
            if (commonMasks[i] and mask.inv() != 0) continue
            counts.copyInto(scratch)
            var fits = true
            for (ch in word) {
                if (--scratch[ch - 'a'] < 0) {
                    fits = false
                    break
                }
            }
            if (fits) result += word
        }
        return result
    }

    companion object {
        const val MAX_LENGTH = 7

        fun parse(language: Language, text: String): Dictionary {
            val common = ArrayList<String>(24_000)
            val valid = arrayOfNulls<String>(MAX_LENGTH + 1)
            var section = ""
            var validLength = 0
            var displayStart = text.length
            var pos = 0
            while (pos < text.length) {
                var end = text.indexOf('\n', pos)
                if (end < 0) end = text.length
                if (text.startsWith("@display", pos)) {
                    displayStart = minOf(end + 1, text.length)
                    break
                }
                when {
                    end == pos || text[pos] == '#' -> Unit
                    text[pos] == '@' -> {
                        val header = text.substring(pos + 1, end).split(' ')
                        section = header[0]
                        if (section == "valid") validLength = header[1].toInt()
                    }
                    section == "common" -> common += text.substring(pos, end)
                    section == "valid" -> if (validLength in 1..MAX_LENGTH) valid[validLength] = text.substring(pos, end)
                }
                pos = end + 1
            }

            val displayBlock = text.substring(displayStart)
            val lines = ArrayList<Int>()
            var start = 0
            while (start < displayBlock.length) {
                var end = displayBlock.indexOf('\n', start)
                if (end < 0) end = displayBlock.length
                if (end > start && displayBlock.indexOf(' ', start).let { it in start until end }) lines += start
                start = end + 1
            }

            val masks = IntArray(common.size) { i ->
                common[i].fold(0) { mask, ch -> mask or (1 shl (ch - 'a')) }
            }
            return Dictionary(language, common, masks, valid, displayBlock, lines.toIntArray())
        }

        private fun compareRegion(block: String, offset: Int, word: String): Int {
            for (i in word.indices) {
                val diff = block[offset + i] - word[i]
                if (diff != 0) return diff
            }
            return 0
        }

        private fun compareKey(block: String, start: Int, end: Int, word: String): Int {
            val length = end - start
            for (i in 0 until minOf(length, word.length)) {
                val diff = block[start + i] - word[i]
                if (diff != 0) return diff
            }
            return length - word.length
        }
    }
}
