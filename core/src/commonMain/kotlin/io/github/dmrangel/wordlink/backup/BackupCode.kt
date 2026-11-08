package io.github.dmrangel.wordlink.backup

import kotlin.io.encoding.Base64

/**
 * Player progress that can move between devices and platforms (Android and
 * the web app). Days are counted since 1970-01-01, like LocalDate.toEpochDay().
 */
data class Backup(
    val coins: Int,
    val jar: Int,
    val wordsFound: Int,
    val bonusFound: Int,
    val streak: Int,
    val lastDaily: Int?,
    /** Next campaign level, by language code ("en", "pt"). */
    val levels: Map<String, Int>,
    /** Day of the last solved daily puzzle, by language code. */
    val dailyDone: Map<String, Int>,
)

/**
 * A short text form of [Backup] the player can copy and paste:
 * "WL1-" + base64url("key=value;..." + 4-byte checksum).
 *
 * Unknown keys are ignored, so later versions can add fields without breaking
 * older readers; a different layout gets a new prefix.
 */
object BackupCode {
    private const val PREFIX = "WL1-"
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun encode(backup: Backup): String {
        val fields = buildList {
            add("c" to backup.coins)
            add("j" to backup.jar)
            add("w" to backup.wordsFound)
            add("b" to backup.bonusFound)
            add("s" to backup.streak)
            backup.lastDaily?.let { add("d" to it) }
            for ((code, level) in backup.levels.entries.sortedBy { it.key }) add("l.$code" to level)
            for ((code, day) in backup.dailyDone.entries.sortedBy { it.key }) add("d.$code" to day)
        }
        val payload = fields.joinToString(";") { (key, value) -> "$key=$value" }.encodeToByteArray()
        return PREFIX + base64.encode(payload + checksum(payload))
    }

    /** The backup in [text], or null if it is not a valid, untampered code. */
    fun decode(text: String): Backup? {
        val compact = text.filterNot { it.isWhitespace() }
        if (!compact.startsWith(PREFIX)) return null
        val bytes = runCatching { base64.decode(compact, PREFIX.length) }.getOrNull() ?: return null
        if (bytes.size < 5) return null
        val payload = bytes.copyOfRange(0, bytes.size - 4)
        if (!checksum(payload).contentEquals(bytes.copyOfRange(bytes.size - 4, bytes.size))) return null

        val values = HashMap<String, Int>()
        for (field in payload.decodeToString().split(';')) {
            val key = field.substringBefore('=', "")
            val value = field.substringAfter('=', "").toIntOrNull() ?: return null
            if (key.isEmpty() || value < 0) return null
            values[key] = value
        }
        fun prefixed(prefix: String) = values.filterKeys { it.startsWith(prefix) }.mapKeys { it.key.removePrefix(prefix) }
        val levels = prefixed("l.")
        if (levels.values.any { it < 1 }) return null
        return Backup(
            coins = values["c"] ?: return null,
            jar = values["j"] ?: 0,
            wordsFound = values["w"] ?: 0,
            bonusFound = values["b"] ?: 0,
            streak = values["s"] ?: 0,
            lastDaily = values["d"],
            levels = levels,
            dailyDone = prefixed("d."),
        )
    }

    /** 32-bit FNV-1a: enough to catch a truncated or mistyped code. */
    private fun checksum(bytes: ByteArray): ByteArray {
        var hash = 0x811C9DC5.toInt()
        for (b in bytes) hash = (hash xor (b.toInt() and 0xFF)) * 0x01000193
        return byteArrayOf((hash ushr 24).toByte(), (hash ushr 16).toByte(), (hash ushr 8).toByte(), hash.toByte())
    }
}
