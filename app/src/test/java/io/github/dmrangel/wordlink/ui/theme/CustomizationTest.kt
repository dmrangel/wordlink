package io.github.dmrangel.wordlink.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CustomizationTest {
    @Test
    fun darkBaseBuildsLighterSurfaces() {
        val colors = customColors(Color(0xFF12161F))
        assertTrue(colors.isDark)
        val ramp = listOf(colors.surface1, colors.surface2, colors.surface3, colors.surface4, colors.surface5)
        assertEquals(ramp.sortedBy { it.luminance() }, ramp)
        assertEquals(Color.White, colors.contrast)
    }

    @Test
    fun lightBaseBuildsLightTheme() {
        val colors = customColors(Color(0xFFF2EEE6))
        assertFalse(colors.isDark)
        assertTrue(colors.contrast.luminance() < 0.1f)
        assertTrue(colors.surface5.luminance() < colors.surface1.luminance())
    }

    @Test
    fun accentPicksReadableTextColor() {
        val green = DarkColors.withCustomization(Customization(accent = 0xFF1BD96A.toInt()))
        assertEquals(Color.Black, green.accentContrast)
        val purple = DarkColors.withCustomization(Customization(accent = 0xFF5B21B6.toInt()))
        assertEquals(Color.White, purple.accentContrast)
        assertEquals(0xFF5B21B6.toInt(), purple.brand.toArgb())
    }

    @Test
    fun backgroundPhotoMakesPanelsTranslucent() {
        val colors = DarkColors.withCustomization(Customization(background = "bg.jpg", panelOpacity = 0.6f))
        assertEquals(0.6f, colors.panel.alpha, 0.01f)
        assertEquals(0.6f, colors.frame.alpha, 0.01f)
        assertEquals(1f, colors.raised.alpha, 0f)
        assertEquals(1f, DarkColors.withCustomization(Customization()).panel.alpha, 0f)
    }

    @Test
    fun hsvRoundTrip() {
        for (color in AccentPresets + ThemeBasePresets) {
            val (h, s, v) = color.toHsv()
            val back = Color.hsv(h, s, v)
            assertTrue(color.toHex(), abs(back.red - color.red) + abs(back.green - color.green) + abs(back.blue - color.blue) < 0.02f)
        }
    }
}
