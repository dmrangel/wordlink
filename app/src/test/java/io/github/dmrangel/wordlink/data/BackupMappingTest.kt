package io.github.dmrangel.wordlink.data

import io.github.dmrangel.wordlink.backup.BackupCode
import io.github.dmrangel.wordlink.game.Language
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BackupMappingTest {
    @Test
    fun profileSurvivesABackupCode() {
        val today = LocalDate.of(2026, 11, 8)
        val profile = Profile(
            coins = 420,
            jar = 3,
            levels = mapOf(Language.English to 57, Language.Portuguese to 1),
            wordsFound = 300,
            bonusFound = 41,
            streak = 6,
            lastDaily = today,
            dailyDone = mapOf(Language.English to today, Language.Portuguese to null),
        )
        assertEquals(profile, BackupCode.decode(BackupCode.encode(profile.toBackup()))!!.toProfile())
    }
}
