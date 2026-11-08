package io.github.dmrangel.wordlink.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class CellShape(val radius: Float) { Square(0.1f), Rounded(0.22f), Circle(0.5f) }

/** Everything the player can tweak on top of the selected theme. */
@Immutable
data class Customization(
    /** Replaces the theme's brand color everywhere; null keeps the theme default. */
    val accent: Int? = null,
    /** Background color the Custom theme is derived from. */
    val themeBase: Int = DEFAULT_THEME_BASE,
    /** File name of the imported background photo, if any. */
    val background: String? = null,
    val blur: Float = 0.35f,
    val dim: Float = 0.35f,
    val panelOpacity: Float = 0.78f,
    val cellShape: CellShape = CellShape.Rounded,
    /** Letter wheel diameter relative to the space available for it. */
    val wheelScale: Float = 1f,
    val leftHanded: Boolean = false,
    val reduceMotion: Boolean = false,
) {
    companion object {
        const val DEFAULT_THEME_BASE = 0xFF12161F.toInt()
    }
}

val LocalCustomization = staticCompositionLocalOf { Customization() }

/** Accent presets: Modrinth's dark palette plus a few extra hues. */
val AccentPresets = listOf(
    Color(0xFF1BD96A), // brand green
    Color(0xFF4F9CFF), // blue
    Color(0xFFC78AFF), // purple
    Color(0xFFFF6BCB), // pink
    Color(0xFFFF496E), // red
    Color(0xFFFFA347), // orange
    Color(0xFFFACC15), // yellow
    Color(0xFF2DD4BF), // teal
)

/** Starting points for the Custom theme: dark tints and two light ones. */
val ThemeBasePresets = listOf(
    Color(0xFF12161F), // midnight
    Color(0xFF1A1322), // plum
    Color(0xFF0F1A14), // forest
    Color(0xFF1D1715), // mocha
    Color(0xFF20232A), // slate
    Color(0xFFF2EEE6), // paper
    Color(0xFFE4ECF4), // mist
)

/**
 * Builds a full surface ramp from one color, using the same lightness steps
 * as Modrinth's own dark (surface-1 #16181C .. surface-5 #42444A) and light
 * (#EBEBEB .. #FFFFFF) themes, so any hue gets the familiar layering.
 */
fun customColors(base: Color): MrColors {
    val (h, s, l) = base.toHsl()
    fun shade(delta: Float) = Color.hsl(h, s, (l + delta).coerceIn(0f, 1f))
    return if (base.luminance() < 0.35f) {
        DarkColors.copy(
            surface1 = base,
            surface2 = shade(0.027f),
            surface3 = shade(0.069f),
            surface4 = shade(0.122f),
            surface5 = shade(0.177f),
            base = Color.hsl(h, min(s, 0.2f), 0.75f),
            secondary = Color.hsl(h, min(s, 0.16f), 0.64f),
            brandGradient = listOf(lerp(shade(0.04f), DarkColors.brand, 0.08f), shade(-0.01f)),
            frame = shade(0.069f),
            panel = base,
        )
    } else {
        LightColors.copy(
            surface1 = base,
            surface2 = shade(0.04f),
            surface3 = shade(0.05f),
            surface4 = shade(0.08f),
            surface5 = shade(-0.055f),
            contrast = Color.hsl(h, min(s, 0.3f), 0.13f),
            base = Color.hsl(h, min(s, 0.08f), 0.18f),
            secondary = Color.hsl(h, min(s, 0.08f), 0.3f),
            brandGradient = listOf(lerp(shade(0.04f), LightColors.brand, 0.12f), shade(0.05f)),
            frame = shade(0.05f),
            panel = base,
        )
    }
}

/** Applies the accent color and background-photo translucency to a palette. */
fun MrColors.withCustomization(custom: Customization): MrColors {
    var colors = this
    custom.accent?.let { argb ->
        val accent = Color(argb)
        colors = colors.copy(
            brand = accent,
            brandHighlight = accent.copy(alpha = 0.25f),
            accentContrast = if (accent.luminance() > 0.4f) Color.Black else Color.White,
            brandGradient = listOf(lerp(colors.surface2, accent, if (isDark) 0.12f else 0.16f), lerp(colors.surface1, accent, 0.03f)),
        )
    }
    if (custom.background != null) {
        colors = colors.copy(
            frame = colors.surface3.copy(alpha = custom.panelOpacity),
            panel = colors.surface1.copy(alpha = custom.panelOpacity),
        )
    }
    return colors
}

/** Hue in degrees, saturation and lightness in 0..1. */
fun Color.toHsl(): Triple<Float, Float, Float> {
    val maxC = max(red, max(green, blue))
    val minC = min(red, min(green, blue))
    val l = (maxC + minC) / 2f
    val d = maxC - minC
    if (d == 0f) return Triple(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    return Triple(hue(maxC, d), s.coerceIn(0f, 1f), l)
}

/** Hue in degrees, saturation and value in 0..1. */
fun Color.toHsv(): Triple<Float, Float, Float> {
    val maxC = max(red, max(green, blue))
    val d = maxC - min(red, min(green, blue))
    val s = if (maxC == 0f) 0f else d / maxC
    return Triple(if (d == 0f) 0f else hue(maxC, d), s, maxC)
}

private fun Color.hue(maxC: Float, d: Float): Float {
    val h = when (maxC) {
        red -> ((green - blue) / d).mod(6f)
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    }
    return (h * 60f).mod(360f)
}

fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)
