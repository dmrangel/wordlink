package io.github.dmrangel.wordlink.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.R
import io.github.dmrangel.wordlink.ui.components.AppBackground
import io.github.dmrangel.wordlink.ui.components.ButtonLabel
import io.github.dmrangel.wordlink.ui.components.ButtonSize
import io.github.dmrangel.wordlink.ui.components.ButtonType
import io.github.dmrangel.wordlink.ui.components.CoinsBadge
import io.github.dmrangel.wordlink.ui.components.Flow
import io.github.dmrangel.wordlink.ui.components.MrButton
import io.github.dmrangel.wordlink.ui.components.MrCard
import io.github.dmrangel.wordlink.ui.components.MrIcon
import io.github.dmrangel.wordlink.ui.components.Txt
import io.github.dmrangel.wordlink.ui.components.pressable
import io.github.dmrangel.wordlink.ui.home.AppLogo
import io.github.dmrangel.wordlink.ui.icons.Lucide
import io.github.dmrangel.wordlink.ui.theme.LocalCustomization
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType
import io.github.dmrangel.wordlink.ui.theme.toHex
import io.github.dmrangel.wordlink.ui.theme.toHsv
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Horizontal slider in the Modrinth style: a pill track, filled with the brand
 * color (or a custom gradient), and a white thumb. Reports every drag step
 * through [onValueChange] and the release through [onFinished].
 */
@Composable
fun MrSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    track: Brush? = null,
    thumbColor: Color? = null,
    enabled: Boolean = true,
    onFinished: () -> Unit = {},
) {
    val colors = Mr.colors
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onFinished)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .semantics { stateDescription = "${(value * 100).roundToInt()}%" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val inset = 12.dp.toPx()
                    fun valueAt(x: Float) = ((x - inset) / (size.width - inset * 2)).coerceIn(0f, 1f)
                    down.consume()
                    change(valueAt(down.position.x))
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) break
                        pointer.consume()
                        change(valueAt(pointer.position.x))
                    }
                    finish()
                }
            },
    ) {
        val alpha = if (enabled) 1f else 0.4f
        val inset = 12.dp.toPx()
        val trackHeight = 8.dp.toPx()
        val y = size.height / 2
        val x = inset + (size.width - inset * 2) * value.coerceIn(0f, 1f)
        val top = Offset(0f, y - trackHeight / 2)
        val trackSize = Size(size.width, trackHeight)
        val corner = CornerRadius(trackHeight / 2)
        if (track != null) {
            drawRoundRect(track, top, trackSize, corner, alpha = alpha)
        } else {
            drawRoundRect(colors.button, top, trackSize, corner, alpha = alpha)
            drawRoundRect(colors.brand, top, Size(x, trackHeight), corner, alpha = alpha)
        }
        val radius = 10.dp.toPx()
        drawCircle(Color.Black.copy(alpha = 0.25f * alpha), radius + 1.5.dp.toPx(), Offset(x, y + 1.dp.toPx()))
        drawCircle(Color.White.copy(alpha = alpha), radius, Offset(x, y))
        drawCircle((thumbColor ?: colors.brand).copy(alpha = alpha), radius - 3.dp.toPx(), Offset(x, y))
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueText: String = "${(value * 100).roundToInt()}%",
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, Modifier.weight(1f), style = MrType.body, color = Mr.colors.contrast)
            Txt(valueText, style = MrType.label, color = Mr.colors.secondary)
        }
        MrSlider(value, onValueChange, enabled = enabled, onFinished = onFinished)
    }
}

/** Round color sample; selected ones get the brand ring and a check mark. */
@Composable
fun Swatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    icon: ImageVector? = null,
    size: Dp = 40.dp,
) {
    val colors = Mr.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size + 8.dp)
            .border(2.dp, if (selected) colors.brand else Color.Transparent, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(color))
            .border(1.dp, colors.surface5, CircleShape)
            .pressable(interaction, CircleShape, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val ink = if (brush != null || color.luminance() > 0.4f) Color.Black else Color.White
        when {
            selected -> MrIcon(Lucide.Check, tint = ink, size = 18.dp)
            icon != null -> MrIcon(icon, tint = ink, size = 18.dp)
        }
    }
}

private val Rainbow = Brush.horizontalGradient(
    listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 1f, 1f) },
)

val RainbowSweep = Brush.sweepGradient(
    listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 0.85f, 1f) },
)

/** Hue / saturation / brightness picker with presets, shown in a modal. */
@Composable
fun ColorPickerCard(title: String, initial: Color, presets: List<Color>, onApply: (Color) -> Unit, onCancel: () -> Unit) {
    val colors = Mr.colors
    val start = remember(initial) { initial.toHsv() }
    var hue by remember(initial) { mutableFloatStateOf(start.first) }
    var saturation by remember(initial) { mutableFloatStateOf(start.second) }
    var brightness by remember(initial) { mutableFloatStateOf(start.third) }
    val picked = Color.hsv(hue.coerceIn(0f, 360f), saturation, brightness)

    MrCard(Modifier.fillMaxWidth(), padding = PaddingValues(MrSize.gapXl)) {
        Column(verticalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                MrIcon(Lucide.Pipette, tint = colors.brand, size = 22.dp)
                Txt(title, Modifier.weight(1f), style = MrType.title, color = colors.contrast)
            }
            Box(
                Modifier.fillMaxWidth().height(64.dp).background(picked, RoundedCornerShape(MrSize.radiusMd))
                    .border(1.dp, colors.surface5, RoundedCornerShape(MrSize.radiusMd)),
                contentAlignment = Alignment.Center,
            ) {
                Txt(picked.toHex(), style = MrType.heading, color = if (picked.luminance() > 0.4f) Color.Black else Color.White)
            }
            Txt(stringResource(R.string.hue), style = MrType.label, color = colors.secondary)
            MrSlider(hue / 360f, { hue = it * 360f }, track = Rainbow, thumbColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f))
            Txt(stringResource(R.string.saturation), style = MrType.label, color = colors.secondary)
            MrSlider(
                saturation,
                { saturation = it },
                track = Brush.horizontalGradient(listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness))),
                thumbColor = picked,
            )
            Txt(stringResource(R.string.brightness), style = MrType.label, color = colors.secondary)
            MrSlider(
                brightness,
                { brightness = it },
                track = Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, saturation, 1f))),
                thumbColor = picked,
            )
            Flow(spacing = MrSize.gapXs) {
                for (preset in presets) {
                    Swatch(preset, selected = false, onClick = {
                        val (h, s, v) = preset.toHsv()
                        hue = h
                        saturation = s
                        brightness = v
                    }, description = preset.toHex(), size = 32.dp)
                }
            }
            Row(Modifier.padding(top = MrSize.gapSm), horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                MrButton(onCancel, Modifier.weight(1f), size = ButtonSize.Xl) {
                    ButtonLabel(stringResource(R.string.cancel), ButtonSize.Xl)
                }
                MrButton({ onApply(picked) }, Modifier.weight(1f), type = ButtonType.Colored, color = picked, size = ButtonSize.Xl) {
                    ButtonLabel(stringResource(R.string.apply), ButtonSize.Xl)
                }
            }
        }
    }
}

/**
 * A miniature of the app with the current theme, accent, photo and tile
 * shape, so every change can be judged without leaving the settings.
 */
@Composable
fun ThemePreview(modifier: Modifier = Modifier) {
    val colors = Mr.colors
    val custom = LocalCustomization.current
    val shape = RoundedCornerShape(MrSize.radiusLg)
    Box(modifier.fillMaxWidth().height(188.dp).clip(shape).border(1.dp, colors.surface5, shape)) {
        AppBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(colors.frame).padding(horizontal = MrSize.gapMd, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppLogo(size = 24.dp)
                Txt(stringResource(R.string.app_name), Modifier.padding(start = MrSize.gapSm).weight(1f), style = MrType.heading, color = colors.contrast)
                CoinsBadge(240)
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(colors.panel)
                    .padding(MrSize.gapMd),
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PreviewCell('L', solved = true)
                            PreviewCell('I', solved = true)
                            PreviewCell('N', hinted = true)
                            PreviewCell('K')
                        }
                        MrButton({}, type = ButtonType.Colored, size = ButtonSize.Sm) {
                            MrIcon(Lucide.Play, size = 14.dp)
                            Txt(stringResource(R.string.play), style = MrType.label, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    PreviewWheel()
                }
            }
        }
    }
}

@Composable
private fun PreviewCell(letter: Char, solved: Boolean = false, hinted: Boolean = false) {
    val colors = Mr.colors
    val shape = RoundedCornerShape(30.dp * LocalCustomization.current.cellShape.radius)
    Box(
        Modifier
            .size(30.dp)
            .background(if (solved) colors.brand else if (hinted) colors.button else colors.raised, shape)
            .border(1.dp, if (solved || hinted) Color.Transparent else colors.surface5, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (solved || hinted) {
            Txt(letter.toString(), style = MrType.heading, color = if (solved) colors.accentContrast else colors.contrast)
        }
    }
}

@Composable
private fun PreviewWheel() {
    val colors = Mr.colors
    val size = 96.dp
    val ring = size / 2 * 0.55f
    // Three letters on the ring at -90°, 30° and 150°; the first two are linked.
    val letters = listOf(Triple("W", -90.0, true), Triple("O", 30.0, true), Triple("D", 150.0, false))
    val points = letters.map { (_, angle, _) ->
        val a = Math.toRadians(angle)
        (size / 2 + ring * cos(a).toFloat()) to (size / 2 + ring * sin(a).toFloat())
    }
    Box(Modifier.size(size).background(colors.raised, CircleShape).border(1.dp, colors.surface5, CircleShape)) {
        Canvas(Modifier.fillMaxSize()) {
            val (a, b) = points.take(2).map { (x, y) -> Offset(x.toPx(), y.toPx()) }
            drawLine(colors.brand, a, b, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(colors.brand, 13.dp.toPx(), a)
            drawCircle(colors.brand, 13.dp.toPx(), b)
        }
        letters.forEachIndexed { i, (letter, _, selected) ->
            val (x, y) = points[i]
            Box(Modifier.offset(x - 13.dp, y - 13.dp).size(26.dp), contentAlignment = Alignment.Center) {
                Txt(letter, style = MrType.heading, color = if (selected) colors.accentContrast else colors.contrast)
            }
        }
    }
}
