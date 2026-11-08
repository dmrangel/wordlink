package io.github.dmrangel.wordlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize

/**
 * The Modrinth App window layout adapted to a phone: a raised frame holds the
 * top bar, and the page sits in a darker panel with rounded top corners and a
 * hairline border (.app-contents in the desktop app).
 */
@Composable
fun AppFrame(
    topBar: @Composable RowScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Mr.colors
    val panel = RoundedCornerShape(topStart = MrSize.radiusXl, topEnd = MrSize.radiusXl)
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.frame)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = MrSize.gapSm),
            verticalAlignment = Alignment.CenterVertically,
            content = topBar,
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(panel)
                .background(colors.panel)
                .drawWithContent {
                    drawContent()
                    val stroke = 1.dp.toPx()
                    val radius = MrSize.radiusXl.toPx()
                    // Taller than the panel so only the top and side edges show.
                    drawRoundRect(
                        color = colors.surface5,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height + radius * 2),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(stroke),
                    )
                }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            content = content,
        )
    }
}
