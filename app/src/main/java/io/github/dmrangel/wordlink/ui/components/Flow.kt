package io.github.dmrangel.wordlink.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Lays children out left to right, wrapping onto new lines. */
@Composable
fun Flow(modifier: Modifier = Modifier, spacing: Dp = 8.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)
        var x = 0
        var y = 0
        var lineHeight = 0
        var width = 0
        for (p in placeables) {
            if (x > 0 && x + p.width > constraints.maxWidth) {
                x = 0
                y += lineHeight + gap
                lineHeight = 0
            }
            positions += x to y
            x += p.width + gap
            lineHeight = maxOf(lineHeight, p.height)
            width = maxOf(width, x - gap)
        }
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), (y + lineHeight).coerceAtLeast(constraints.minHeight)) {
            placeables.forEachIndexed { i, p -> p.place(positions[i].first, positions[i].second) }
        }
    }
}
