package io.github.dmrangel.wordlink.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.dmrangel.wordlink.game.Cell
import io.github.dmrangel.wordlink.game.GameSession
import io.github.dmrangel.wordlink.ui.components.pressable
import io.github.dmrangel.wordlink.ui.theme.LocalCustomization
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrType
import kotlinx.coroutines.delay

private enum class CellState { Hidden, Hinted, Solved }

@Composable
fun CrosswordGrid(session: GameSession, onTarget: (Cell) -> Unit, modifier: Modifier = Modifier) {
    val level = session.level
    BoxWithConstraints(modifier.testTag("grid"), contentAlignment = Alignment.Center) {
        val gapRatio = 0.12f
        val byWidth = maxWidth / (level.cols + (level.cols - 1) * gapRatio)
        val byHeight = maxHeight / (level.rows + (level.rows - 1) * gapRatio)
        val cell = min(min(byWidth, byHeight), 58.dp)
        val step = cell * (1 + gapRatio)
        // One shared pulse for the Target outline instead of a conditional
        // infinite transition per cell: toggling that per-cell composable in and
        // out left the hidden tiles undrawn after targeting ended.
        val targetPulse = remember { Animatable(1f) }
        LaunchedEffect(session.targeting) {
            if (!session.targeting) {
                targetPulse.snapTo(1f)
                return@LaunchedEffect
            }
            while (true) {
                targetPulse.animateTo(0.35f, tween(650))
                targetPulse.animateTo(1f, tween(650))
            }
        }
        Box(Modifier.size(step * (level.cols - 1) + cell, step * (level.rows - 1) + cell)) {
            val solved = session.solvedCells
            val hinted = session.hinted
            val pulse = session.pulse
            for ((position, letter) in level.letterAt) {
                val state = when (position) {
                    in solved -> CellState.Solved
                    in hinted -> CellState.Hinted
                    else -> CellState.Hidden
                }
                GridCell(
                    letter = letter,
                    state = state,
                    size = cell,
                    delaySlot = session.revealDelay[position] ?: 0,
                    pulseId = if (pulse != null && position in level.cellsOf.getValue(pulse.word)) pulse.id else null,
                    targeting = session.targeting && state == CellState.Hidden,
                    targetPulse = { targetPulse.value },
                    onTap = { onTarget(position) },
                    modifier = Modifier.offset(step * position.col, step * position.row),
                )
            }
        }
    }
}

@Composable
private fun GridCell(
    letter: Char,
    state: CellState,
    size: Dp,
    delaySlot: Int,
    pulseId: Long?,
    targeting: Boolean,
    targetPulse: () -> Float,
    onTap: () -> Unit,
    modifier: Modifier,
) {
    val colors = Mr.colors
    val density = LocalDensity.current
    val custom = LocalCustomization.current
    val shape = RoundedCornerShape(size * custom.cellShape.radius)
    val scale = remember { Animatable(1f) }
    val shown = remember { booleanArrayOf(state != CellState.Hidden) }

    LaunchedEffect(state) {
        if (state != CellState.Hidden && !shown[0]) {
            shown[0] = true
            if (custom.reduceMotion) return@LaunchedEffect
            delay(delaySlot * 55L)
            scale.snapTo(0.35f)
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
        }
    }
    LaunchedEffect(pulseId) {
        if (pulseId != null && !custom.reduceMotion) {
            scale.animateTo(1.18f, tween(110))
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
    }

    val background by animateColorAsState(
        when (state) {
            CellState.Solved -> colors.brand
            CellState.Hinted -> colors.button
            CellState.Hidden -> colors.raised
        },
        tween(180, delayMillis = if (custom.reduceMotion) 0 else delaySlot * 55),
        label = "cell",
    )
    val borderColor = when {
        targeting -> colors.brand.copy(alpha = targetPulse())
        state == CellState.Hidden -> colors.surface5
        else -> Color.Transparent
    }

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(background, shape)
            .border(if (targeting) 2.dp else 1.dp, borderColor, shape)
            .pressable(interaction, shape, enabled = targeting, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (state != CellState.Hidden) {
            BasicText(
                text = letter.uppercase(),
                style = TextStyle(
                    fontFamily = MrType.title.fontFamily,
                    fontWeight = MrType.title.fontWeight,
                    fontSize = with(density) { (size * 0.56f).toSp() },
                    color = if (state == CellState.Solved) colors.accentContrast else colors.contrast,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
