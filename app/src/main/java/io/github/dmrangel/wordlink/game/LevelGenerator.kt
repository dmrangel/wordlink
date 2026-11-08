package io.github.dmrangel.wordlink.game

import java.time.LocalDate
import kotlin.random.Random

data class Difficulty(
    val letters: Int,
    val minWords: Int,
    val maxWords: Int,
    /** Only words ranked below this may become crossword answers. */
    val answerRank: Int,
    val maxRows: Int,
    val maxCols: Int,
) {
    companion object {
        fun forCampaign(number: Int): Difficulty = when {
            number <= 6 -> Difficulty(4, 3, 5, 2_500, 6, 7)
            number <= 20 -> Difficulty(5, 4, 7, 4_500, 8, 8)
            number <= 50 -> Difficulty(6, 5, 9, 7_000, 9, 9)
            number % 7 == 0 -> Difficulty(6, 6, 10, 9_000, 9, 10) // a breather between 7-letter levels
            else -> Difficulty(
                letters = 7,
                minWords = 6,
                maxWords = minOf(13, 8 + (number - 50) / 40),
                answerRank = minOf(20_000, 8_000 + (number - 50) * 25),
                maxRows = 10,
                maxCols = 10,
            )
        }

        val Daily = Difficulty(7, 7, 12, 12_000, 10, 10)

        /** The most permissive campaign settings for a word length. */
        fun easiestFor(letters: Int): Difficulty = when (letters) {
            4 -> forCampaign(1)
            5 -> forCampaign(7)
            6 -> forCampaign(21)
            else -> forCampaign(51)
        }
    }
}

/**
 * Deterministic level generator: the same dictionary, language and level
 * number always produce the same puzzle, so nothing but the level number has
 * to be stored and the campaign never runs out of levels.
 */
class LevelGenerator(val dictionary: Dictionary) {
    private val buckets = HashMap<Int, List<String>>()

    fun campaign(number: Int): Level {
        val difficulty = Difficulty.forCampaign(number)
        val bucket = bucket(difficulty.letters)
        val position = (1 until number).count { Difficulty.forCampaign(it).letters == difficulty.letters }
        val random = Random(seed(number.toLong()))
        return generate(LevelId.Campaign(dictionary.language, number), difficulty, bucket, position, random)
    }

    fun daily(date: LocalDate): Level {
        val bucket = bucket(Difficulty.Daily.letters)
        val random = Random(seed(1_000_000_000L + date.toEpochDay()))
        val position = random.nextInt(bucket.size)
        return generate(LevelId.Daily(dictionary.language, date), Difficulty.Daily, bucket, position, random)
    }

    private fun generate(id: LevelId, difficulty: Difficulty, bucket: List<String>, position: Int, random: Random): Level {
        for (relaxed in listOf(false, true)) {
            val minWords = if (relaxed) 2 else difficulty.minWords
            for (attempt in 0 until minOf(bucket.size, if (relaxed) bucket.size else 80)) {
                // Levels walk the bucket from the front; the rare fallback is taken
                // from the back, far away from the bases of the neighboring levels.
                val index = if (attempt == 0) position % bucket.size else bucket.size - 1 - (position * 4 + attempt - 1) % bucket.size
                val base = bucket[index]
                val layout = layoutFor(base, difficulty, minWords, random) ?: continue
                return Level(id, wheelOrder(base, layout, random), layout.placements, layout.rows, layout.cols)
            }
        }
        error("No playable level for $id")
    }

    private fun layoutFor(base: String, difficulty: Difficulty, minWords: Int, random: Random): Layout? {
        val answers = dictionary.subAnagrams(base, difficulty.answerRank, MIN_WORD_LENGTH).toMutableList()
        answers.remove(base)
        answers.add(0, base)
        if (answers.size < minWords) return null
        val candidates = answers.take(difficulty.maxWords + 6)
        val layout = CrosswordBuilder.build(candidates, difficulty.maxWords, difficulty.maxRows, difficulty.maxCols, random)
        return layout?.takeIf { it.placements.size >= minWords }
    }

    private fun wheelOrder(base: String, layout: Layout, random: Random): String {
        val words = layout.placements.mapTo(HashSet()) { it.word }
        var letters = base
        repeat(12) {
            letters = base.toList().shuffled(random).joinToString("")
            if (letters !in words) return letters
        }
        return letters
    }

    /**
     * Candidate base words of one length. Only one word per set of letters is
     * kept (anagrams like "four"/"our" would be the same puzzle twice), and
     * only words that can yield enough answers. Sorted by rank in tiers of 300
     * and shuffled within each tier, so early levels draw from the most
     * familiar words while consecutive levels still feel unrelated.
     */
    private fun bucket(letters: Int): List<String> = buckets.getOrPut(letters) {
        val rankLimit = when (letters) {
            4 -> 2_500
            5 -> 4_500
            6 -> 7_000
            else -> 10_000
        }
        val easiest = Difficulty.easiestFor(letters)
        val letterSets = HashSet<String>()
        val random = Random(seed(letters * 31L))
        dictionary.common.asSequence()
            .take(rankLimit)
            .filter { it.length == letters }
            .filter { letterSets.add(it.toCharArray().sorted().joinToString("")) }
            .filter { dictionary.subAnagrams(it, easiest.answerRank, MIN_WORD_LENGTH).size >= easiest.minWords }
            .chunked(300)
            .flatMap { it.shuffled(random) }
            .toList()
    }

    private fun seed(value: Long): Long = (dictionary.language.ordinal + 1) * -0x61c8864680b583ebL + value

    companion object {
        const val MIN_WORD_LENGTH = 3
    }
}
