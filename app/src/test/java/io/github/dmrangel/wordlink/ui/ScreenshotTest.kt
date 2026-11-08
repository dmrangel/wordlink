package io.github.dmrangel.wordlink.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.core.content.edit
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.game.WordResult
import io.github.dmrangel.wordlink.ui.theme.CellShape
import io.github.dmrangel.wordlink.ui.theme.Customization
import io.github.dmrangel.wordlink.ui.theme.ThemeMode
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the real screens on the JVM and saves them to app/build/screenshots.
 * Doubles as a smoke test: every screen must compose, lay out and draw.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-xhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val out = File("build/screenshots").apply { mkdirs() }

    @Test
    fun home() {
        launch(ThemeMode.Dark, Language.Portuguese, level = 37)
        shoot("home_pt_dark")
    }

    @Test
    fun settings() {
        val vm = launch(ThemeMode.Dark, Language.Portuguese)
        compose.runOnIdle { vm.navigate(Screen.Settings) }
        shoot("settings_pt_dark")
    }

    @Test
    fun gamePortugueseDark() {
        val vm = play(ThemeMode.Dark, Language.Portuguese, level = 57)
        val session = vm.session!!
        compose.runOnIdle {
            session.level.words.drop(1).take(4).forEach { session.submit(it) }
            session.hint(AppViewModel.HINT_COST)
        }
        drag(session.level.letters.length, slots = listOf(0, 2, 3, 5))
        shoot("game_pt_dark")
    }

    @Test
    fun gameEnglishLight() {
        val vm = play(ThemeMode.Light, Language.English, level = 120)
        val session = vm.session!!
        compose.runOnIdle { session.level.words.take(3).forEach { session.submit(it) } }
        shoot("game_en_light")
    }

    @Test
    fun levelComplete() {
        val vm = play(ThemeMode.Oled, Language.English, level = 9)
        val session = vm.session!!
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { session.level.words.forEach { session.submit(it) } }
        compose.mainClock.advanceTimeBy(1_400)
        assertNotNull(vm.completion)
        shoot("complete_en_oled")
    }

    /** Regression: cancelling or using Target used to leave the hidden tiles undrawn. */
    @Test
    fun targetingKeepsTilesVisible() {
        val vm = play(ThemeMode.Dark, Language.Portuguese, level = 30)
        val session = vm.session!!
        compose.runOnIdle { session.submit(session.level.words.first()) }
        compose.waitForIdle()
        val before = gridPixels()

        compose.mainClock.autoAdvance = false
        compose.runOnIdle { session.targeting = true }
        compose.mainClock.advanceTimeBy(700)
        val targeting = gridPixels() // a frame must be drawn while targeting, as on a phone
        assertTrue("Target should highlight the hidden tiles", targeting.indices.count { targeting[it] != before[it] } > before.size / 200)
        compose.runOnIdle { session.targeting = false }
        compose.mainClock.advanceTimeBy(700)
        val after = gridPixels()
        val changed = before.indices.count { before[it] != after[it] }
        assertTrue("$changed of ${before.size} grid pixels changed after cancelling Target", changed < before.size / 200)

        compose.runOnIdle { session.targeting = true }
        compose.mainClock.advanceTimeBy(700)
        val hidden = session.level.letterAt.keys.first { it !in session.solvedCells && it !in session.hinted }
        compose.runOnIdle { session.target(hidden, AppViewModel.TARGET_COST) }
        compose.mainClock.advanceTimeBy(1_000)
        shoot("target_used")
    }

    /** The grid area cut from a capture of the whole window, as the player sees it. */
    private fun gridPixels(): IntArray {
        val bounds = compose.onNodeWithTag("grid").fetchSemanticsNode().boundsInRoot
        val screen = compose.onRoot().captureToImage().asAndroidBitmap()
        val x = bounds.left.toInt()
        val y = bounds.top.toInt()
        val w = bounds.width.toInt().coerceAtMost(screen.width - x)
        val h = bounds.height.toInt().coerceAtMost(screen.height - y)
        return IntArray(w * h).also { screen.getPixels(it, 0, w, x, y, w, h) }
    }

    @Test
    fun bonusDialogPortuguese() {
        val vm = play(ThemeMode.Dark, Language.Portuguese, level = 60)
        val session = vm.session!!
        compose.runOnIdle {
            val letters = session.level.letters
            // Spell short letter combinations until two of them are bonus words.
            val combos = letters.indices.flatMap { a -> letters.indices.flatMap { b -> letters.indices.map { c -> listOf(a, b, c) } } }
                .filter { it.toSet().size == 3 }
                .map { idx -> idx.joinToString("") { letters[it].toString() } }
            var bonus = 0
            for (word in combos) {
                if (bonus == 2) break
                if (session.submit(word) == WordResult.Bonus) bonus++
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Bônus").performClick()
        compose.waitForIdle()
        shoot("bonus_pt")
    }

    @Test
    fun interfaceFollowsGameLanguage() {
        val vm = launch(ThemeMode.Dark, Language.English)
        compose.onNodeWithText("Play").assertExists()
        compose.runOnIdle { vm.update { it.copy(language = Language.Portuguese) } }
        compose.onNodeWithText("Jogar").assertExists()
        compose.onNodeWithText("Desafio diário").assertExists()
    }

    @Test
    fun customizedGame() {
        val custom = Customization(
            accent = 0xFFFF6BCB.toInt(),
            themeBase = 0xFF1A1322.toInt(),
            background = samplePhoto(),
            blur = 0.5f,
            dim = 0.3f,
            panelOpacity = 0.62f,
            cellShape = CellShape.Circle,
        )
        val vm = play(ThemeMode.Custom, Language.Portuguese, level = 30, custom = custom)
        val session = vm.session!!
        compose.runOnIdle { session.level.words.take(4).forEach { session.submit(it) } }
        drag(session.level.letters.length, slots = listOf(1, 0, 4))
        shoot("game_custom")
    }

    @Test
    fun settingsCustomization() {
        val custom = Customization(accent = 0xFFC78AFF.toInt(), background = samplePhoto(), panelOpacity = 0.7f)
        val vm = launch(ThemeMode.Dark, Language.Portuguese, custom = custom)
        compose.runOnIdle { vm.navigate(Screen.Settings) }
        compose.onNodeWithText("Cor de destaque").performScrollTo()
        compose.waitForIdle()
        shoot("settings_customization")
        compose.onNodeWithText("Restaurar personalização").performScrollTo()
        compose.waitForIdle()
        shoot("settings_customization_board")
    }

    /** A synthetic "photo": a dusk gradient with soft light spots. */
    private fun samplePhoto(): String {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, 1920f, intArrayOf(0xFF2B1055.toInt(), 0xFFD53369.toInt(), 0xFFF7B267.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 1080f, 1920f, paint)
        paint.shader = null
        val spots = listOf(Triple(220f, 420f, 0x55FFFFFF), Triple(820f, 760f, 0x44FFE29A), Triple(480f, 1380f, 0x33FFFFFF), Triple(900f, 1650f, 0x55FF8FB1))
        for ((x, y, color) in spots) {
            paint.color = color
            canvas.drawCircle(x, y, 170f, paint)
        }
        val dir = File(compose.activity.filesDir, "backgrounds").apply { mkdirs() }
        File(dir, "bg-test.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return "bg-test.jpg"
    }

    private fun launch(theme: ThemeMode, language: Language, level: Int = 1, custom: Customization = Customization()): AppViewModel {
        val app = compose.activity.application
        app.getSharedPreferences("wordlink", 0).edit {
            putInt("level_${language.code}", level)
            putInt("coins", 240)
            putInt("wordsFound", 412)
            putInt("bonusFound", 57)
        }
        val vm = AppViewModel(app)
        vm.update { it.copy(theme = theme, language = language, sound = false, haptics = false, custom = custom) }
        val fx = Fx(compose.activity)
        compose.setContent { WordlinkRoot(vm, fx) }
        return vm
    }

    private fun play(theme: ThemeMode, language: Language, level: Int, custom: Customization = Customization()): AppViewModel {
        val vm = launch(theme, language, level, custom)
        compose.runOnIdle { vm.play() }
        // The level is generated off the main thread; keep the paused main looper moving.
        val deadline = System.currentTimeMillis() + 20_000
        while (vm.session == null && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        assertNotNull(vm.session)
        compose.waitForIdle()
        return vm
    }

    /** Presses the wheel and slides across a few letters without lifting. */
    private fun drag(count: Int, slots: List<Int>) {
        compose.onNodeWithTag("wheel").performTouchInput {
            val radius = width / 2f
            fun at(slot: Int): Offset {
                val angle = -PI / 2 + slot * 2 * PI / count
                return Offset(radius + 0.62f * radius * cos(angle).toFloat(), radius + 0.62f * radius * sin(angle).toFloat())
            }
            down(at(slots.first()))
            for (slot in slots.drop(1)) moveTo(at(slot))
            moveBy(Offset(radius * 0.12f, radius * 0.18f))
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
