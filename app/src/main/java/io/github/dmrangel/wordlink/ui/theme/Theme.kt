package io.github.dmrangel.wordlink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dmrangel.wordlink.R

/**
 * Color tokens from the Modrinth design system (packages/assets/styles/variables.scss
 * in github.com/modrinth/code). Names follow the CSS variables:
 * surface-1..5, --color-text-primary/default/tertiary, --color-brand, etc.
 */
@Immutable
data class MrColors(
    val isDark: Boolean,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    val surface5: Color,
    /** --color-text-primary (headings, emphasis). */
    val contrast: Color,
    /** --color-text-default (body text, icons). */
    val base: Color,
    /** --color-text-tertiary (captions, hints). */
    val secondary: Color,
    val brand: Color,
    val brandHighlight: Color,
    val red: Color,
    val orange: Color,
    val blue: Color,
    val purple: Color,
    val accentContrast: Color,
    /** --brand-gradient-strong-bg, left and right stops. */
    val brandGradient: List<Color>,
    /** --hover-brightness, used here for pressed states. */
    val pressOverlay: Color,
    /** The app frame behind the top bar; translucent over a background photo. */
    val frame: Color = surface3,
    /** The rounded content panel; translucent over a background photo. */
    val panel: Color = surface1,
) {
    /** --color-raised-bg: cards and the app frame. */
    val raised: Color get() = surface3

    /** --color-bg: the main content area. */
    val background: Color get() = surface1

    /** --color-button-bg. */
    val button: Color get() = surface4

    fun highlight(color: Color): Color = color.copy(alpha = 0.25f)
}

val DarkColors = MrColors(
    isDark = true,
    surface1 = Color(0xFF16181C),
    surface2 = Color(0xFF1D1F23),
    surface3 = Color(0xFF27292E),
    surface4 = Color(0xFF34363C),
    surface5 = Color(0xFF42444A),
    contrast = Color(0xFFFFFFFF),
    base = Color(0xFFB0BAC5),
    secondary = Color(0xFF96A2B0),
    brand = Color(0xFF1BD96A),
    brandHighlight = Color(0x401BD96A),
    red = Color(0xFFFF496E),
    orange = Color(0xFFFFA347),
    blue = Color(0xFF4F9CFF),
    purple = Color(0xFFC78AFF),
    accentContrast = Color(0xFF000000),
    brandGradient = listOf(Color(0xFF131F17), Color(0xFF09110D)),
    pressOverlay = Color(0x1AFFFFFF),
)

val OledColors = MrColors(
    isDark = true,
    surface1 = Color(0xFF000000),
    surface2 = Color(0xFF09090A),
    surface3 = Color(0xFF101013),
    surface4 = Color(0xFF1B1B20),
    surface5 = Color(0xFF25262B),
    contrast = DarkColors.contrast,
    base = DarkColors.base,
    secondary = DarkColors.secondary,
    brand = DarkColors.brand,
    brandHighlight = DarkColors.brandHighlight,
    red = DarkColors.red,
    orange = DarkColors.orange,
    blue = DarkColors.blue,
    purple = DarkColors.purple,
    accentContrast = DarkColors.accentContrast,
    brandGradient = listOf(Color(0x80131F17), Color(0x9909120E)),
    pressOverlay = Color(0x1AFFFFFF),
)

val LightColors = MrColors(
    isDark = false,
    surface1 = Color(0xFFEBEBEB),
    surface2 = Color(0xFFF5F5F5),
    surface3 = Color(0xFFF8F8F8),
    surface4 = Color(0xFFFFFFFF),
    surface5 = Color(0xFFDDDDDD),
    contrast = Color(0xFF1A202C),
    base = Color(0xFF2C2E31),
    secondary = Color(0xFF484D54),
    brand = Color(0xFF00AF5C),
    brandHighlight = Color(0x4000AF5C),
    red = Color(0xFFCB2245),
    orange = Color(0xFFE08325),
    blue = Color(0xFF1F68C0),
    purple = Color(0xFF8E32F3),
    accentContrast = Color(0xFFFFFFFF),
    brandGradient = listOf(Color(0x1F24E15B), Color(0x2D44B68A)),
    pressOverlay = Color(0x14000000),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

/** Type scale from defaults.scss: headings are extra-bold, body text is medium. */
object MrType {
    val display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 52.sp)
    val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp)
    val heading = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp)
}

/** --radius-* and --gap-* tokens. */
object MrSize {
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 20.dp
    val gapXs = 4.dp
    val gapSm = 8.dp
    val gapMd = 12.dp
    val gapLg = 16.dp
    val gapXl = 24.dp
}

enum class ThemeMode { Dark, Oled, Light, System, Custom }

val LocalMrColors = staticCompositionLocalOf { DarkColors }
val LocalContentColor = compositionLocalOf { Color.Unspecified }

object Mr {
    val colors: MrColors
        @Composable @ReadOnlyComposable get() = LocalMrColors.current
}

@Composable
fun WordlinkTheme(mode: ThemeMode, custom: Customization = Customization(), content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val colors = remember(mode, custom, systemDark) {
        val base = when (mode) {
            ThemeMode.Dark -> DarkColors
            ThemeMode.Oled -> OledColors
            ThemeMode.Light -> LightColors
            ThemeMode.System -> if (systemDark) DarkColors else LightColors
            ThemeMode.Custom -> customColors(Color(custom.themeBase))
        }
        base.withCustomization(custom)
    }
    CompositionLocalProvider(
        LocalMrColors provides colors,
        LocalContentColor provides colors.base,
        LocalCustomization provides custom,
        content = content,
    )
}
