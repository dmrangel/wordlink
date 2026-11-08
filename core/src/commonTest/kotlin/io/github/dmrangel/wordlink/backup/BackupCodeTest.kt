package io.github.dmrangel.wordlink.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupCodeTest {
    private val sample = Backup(
        coins = 1234,
        jar = 7,
        wordsFound = 812,
        bonusFound = 95,
        streak = 4,
        lastDaily = 20_765,
        levels = mapOf("en" to 57, "pt" to 12),
        dailyDone = mapOf("en" to 20_765),
    )

    @Test
    fun roundTrips() {
        val code = BackupCode.encode(sample)
        assertTrue(code.startsWith("WL1-"))
        assertTrue(code.length < 120, code)
        assertEquals(sample, BackupCode.decode(code))
        assertEquals(sample.copy(lastDaily = null, dailyDone = emptyMap()), BackupCode.decode(BackupCode.encode(sample.copy(lastDaily = null, dailyDone = emptyMap()))))
    }

    /** The format is shared with code that is already installed: it must never drift. */
    @Test
    fun knownCodeStillDecodes() {
        assertEquals(KNOWN, BackupCode.encode(sample))
        assertEquals(sample, BackupCode.decode(KNOWN))
    }

    @Test
    fun toleratesWhitespaceFromCopyPaste() {
        val code = BackupCode.encode(sample)
        assertEquals(sample, BackupCode.decode("  " + code.chunked(16).joinToString("\n ") + "\n"))
    }

    @Test
    fun rejectsDamagedCodes() {
        val code = BackupCode.encode(sample)
        assertNull(BackupCode.decode(""))
        assertNull(BackupCode.decode("hello"))
        assertNull(BackupCode.decode(code.dropLast(3)))
        val flipped = code.substring(0, 10) + (if (code[10] == 'A') 'B' else 'A') + code.substring(11)
        assertNull(BackupCode.decode(flipped))
        assertNull(BackupCode.decode("WL1-!!!!"))
    }

    private companion object {
        const val KNOWN = "WL1-Yz0xMjM0O2o9Nzt3PTgxMjtiPTk1O3M9NDtkPTIwNzY1O2wuZW49NTc7bC5wdD0xMjtkLmVuPTIwNzY1d7OrWg"
    }
}
