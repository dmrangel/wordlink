package io.github.dmrangel.wordlink.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.ui.theme.LocalCustomization
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * The circle of letters. Drag across letters to spell a word; dragging back
 * onto the previous letter undoes the last one. Everything is drawn on a
 * single Canvas, and the finger position is only read while drawing, so a
 * drag redraws one layer per frame without recomposing anything.
 */
@Composable
fun LetterWheel(
    letters: String,
    order: List<Int>,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onLetter: (count: Int) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Mr.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val count = letters.length
    val selected = remember(letters) { mutableStateListOf<Int>() }
    val pointer = remember { mutableStateOf(Offset.Unspecified) }
    val angles = remember(letters) { List(count) { i -> Animatable(slotAngle(order.indexOf(i), count)) } }
    val currentOnChange = rememberUpdatedState(onChange)
    val currentOnLetter = rememberUpdatedState(onLetter)
    val currentOnSubmit = rememberUpdatedState(onSubmit)

    val reduceMotion = LocalCustomization.current.reduceMotion

    // Shuffling glides every letter along the ring to its new slot.
    LaunchedEffect(order) {
        coroutineScope {
            angles.forEachIndexed { i, angle ->
                launch {
                    var target = slotAngle(order.indexOf(i), count)
                    while (target - angle.value > PI) target -= (2 * PI).toFloat()
                    while (angle.value - target > PI) target += (2 * PI).toFloat()
                    if (reduceMotion) angle.snapTo(target) else angle.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 220f))
                }
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            selected.clear()
            pointer.value = Offset.Unspecified
        }
    }

    fun word() = selected.joinToString("") { letters[it].toString() }

    Canvas(
        modifier = modifier.pointerInput(letters, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                val radius = size.width / 2f
                fun hit(position: Offset): Int? {
                    val letterRadius = radius * LETTER_SIZE
                    for (i in 0 until count) {
                        val center = letterCenter(angles[i].value, radius)
                        if (hypot(position.x - center.x, position.y - center.y) < letterRadius * 1.08f) return i
                    }
                    return null
                }
                val first = hit(down.position) ?: return@awaitEachGesture
                down.consume()
                selected.clear()
                selected += first
                pointer.value = down.position
                currentOnLetter.value(1)
                currentOnChange.value(word())
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    pointer.value = change.position
                    val index = hit(change.position) ?: continue
                    if (index !in selected) {
                        selected += index
                        currentOnLetter.value(selected.size)
                        currentOnChange.value(word())
                    } else if (selected.size >= 2 && selected[selected.lastIndex - 1] == index) {
                        selected.removeAt(selected.lastIndex)
                        currentOnChange.value(word())
                    }
                }
                val result = word()
                selected.clear()
                pointer.value = Offset.Unspecified
                currentOnSubmit.value(result)
            }
        },
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(radius, radius)
        val letterRadius = radius * LETTER_SIZE
        val style = TextStyle(
            fontFamily = MrType.title.fontFamily,
            fontWeight = MrType.title.fontWeight,
            fontSize = with(density) { (letterRadius * 1.15f).toSp() },
        )

        drawCircle(colors.raised, radius - 1.dp.toPx(), center)
        drawCircle(colors.surface5, radius - 1.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))

        if (selected.isNotEmpty()) {
            val path = Path()
            selected.forEachIndexed { i, index ->
                val p = letterCenter(angles[index].value, radius)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            val finger = pointer.value
            if (finger != Offset.Unspecified) path.lineTo(finger.x, finger.y)
            drawPath(
                path,
                colors.brand,
                style = Stroke(width = letterRadius * 0.34f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        for (i in 0 until count) {
            val p = letterCenter(angles[i].value, radius)
            val isSelected = i in selected
            if (isSelected) drawCircle(colors.brand, letterRadius, p)
            val layout = measurer.measure(letters[i].uppercase(), style, constraints = Constraints())
            drawText(
                layout,
                color = if (isSelected) colors.accentContrast else colors.contrast,
                topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f),
            )
        }
    }
}

private const val LETTER_SIZE = 0.2f

private fun slotAngle(slot: Int, count: Int): Float = (-PI / 2 + slot * 2 * PI / count).toFloat()

private fun letterCenter(angle: Float, radius: Float): Offset {
    val ring = radius * 0.62f
    return Offset(radius + ring * cos(angle), radius + ring * sin(angle))
}
