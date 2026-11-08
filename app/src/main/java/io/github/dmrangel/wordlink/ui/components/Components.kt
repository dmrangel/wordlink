package io.github.dmrangel.wordlink.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.ui.theme.LocalContentColor
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MrType.body,
    color: Color = Color.Unspecified,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val resolved = if (color.isSpecified) color else LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolved, textAlign = align ?: style.textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun MrIcon(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = Color.Unspecified, size: Dp = 20.dp) {
    val resolved = if (tint.isSpecified) tint else LocalContentColor.current
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(resolved),
    )
}

/** Press feedback shared by every tappable surface: scale 0.97 plus the hover-brightness tint. */
fun Modifier.pressable(
    interaction: MutableInteractionSource,
    shape: Shape,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = this
    .clip(shape)
    .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role, onClick = onClick)

enum class ButtonType { Base, Colored, Quiet, Outlined }

/** Sizes of Modrinth's ButtonFrame (h-8 .. h-12, rounded-[10px] .. rounded-2xl). */
enum class ButtonSize(val height: Dp, val radius: Dp, val padding: Dp, val icon: Dp, val gap: Dp) {
    Sm(32.dp, 10.dp, 10.dp, 16.dp, 4.dp),
    Md(36.dp, 12.dp, 12.dp, 20.dp, 6.dp),
    Lg(40.dp, 14.dp, 16.dp, 20.dp, 8.dp),
    Xl(48.dp, 16.dp, 18.dp, 22.dp, 8.dp),
    Xxl(56.dp, 18.dp, 22.dp, 24.dp, 10.dp),
}

@Composable
fun MrButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.Base,
    color: Color = Mr.colors.brand,
    size: ButtonSize = ButtonSize.Lg,
    enabled: Boolean = true,
    circular: Boolean = false,
    iconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = Mr.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, spring(stiffness = 1500f), label = "press")
    val shape = if (circular) CircleShape else RoundedCornerShape(size.radius)
    val background = when (type) {
        ButtonType.Base -> colors.button
        ButtonType.Colored -> color
        ButtonType.Quiet, ButtonType.Outlined -> Color.Transparent
    }
    val contentColor = when (type) {
        ButtonType.Base -> colors.contrast
        ButtonType.Colored -> colors.accentContrast
        ButtonType.Quiet -> if (color == colors.brand) colors.base else color
        ButtonType.Outlined -> color
    }
    val border = when (type) {
        ButtonType.Base -> Modifier.border(1.dp, colors.surface5, shape)
        ButtonType.Colored -> Modifier.border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)),
            shape,
        )
        ButtonType.Outlined -> Modifier.border(1.dp, color, shape)
        ButtonType.Quiet -> Modifier
    }
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .height(size.height)
            .then(if (iconOnly) Modifier.width(size.height) else Modifier)
            .background(background, shape)
            .then(border)
            .pressable(interaction, shape, enabled, onClick = onClick)
            .drawWithContent {
                drawContent()
                if (pressed) drawRect(colors.pressOverlay)
            }
            .padding(horizontal = if (iconOnly) 0.dp else size.padding),
        horizontalArrangement = Arrangement.spacedBy(size.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun ButtonLabel(text: String, size: ButtonSize = ButtonSize.Lg) {
    Txt(text, style = if (size >= ButtonSize.Xl) MrType.body.copy(fontWeight = MrType.title.fontWeight) else MrType.label.copy(fontSize = MrType.body.fontSize), maxLines = 1)
}

/** Round navigation button from the Modrinth App sidebar (w-12 h-12 rounded-full). */
@Composable
fun NavButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    val colors = Mr.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        when {
            active -> colors.brandHighlight
            pressed -> colors.button
            else -> Color.Transparent
        },
        tween(120),
        label = "nav",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .background(background, CircleShape)
            .pressable(interaction, CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MrIcon(icon, tint = if (active) colors.brand else if (pressed) colors.contrast else colors.base, size = 24.dp)
    }
}

@Composable
fun MrCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(MrSize.gapLg),
    background: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Mr.colors
    val shape = RoundedCornerShape(MrSize.radiusLg)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, spring(stiffness = 1500f), label = "card")
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.raised)
            .then(if (background != null) Modifier.background(background) else Modifier)
            .then(if (onClick != null) Modifier.pressable(interaction, shape, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Pill-shaped chip; selected uses --color-button-bg-selected / --color-button-text-selected. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = Mr.colors
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (selected) colors.brandHighlight else colors.button, tween(150), label = "chip")
    val content by animateColorAsState(if (selected) colors.brand else colors.base, tween(150), label = "chipText")
    Row(
        modifier = modifier
            .height(36.dp)
            .background(background, CircleShape)
            .pressable(interaction, CircleShape, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) MrIcon(icon, tint = content, size = 16.dp)
        Txt(text, style = MrType.label, color = content, maxLines = 1)
    }
}

/** Small rounded label, like the app's .badge. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = Mr.colors.base, background: Color = Mr.colors.button, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(MrSize.radiusMd))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) MrIcon(icon, tint = color, size = 16.dp)
        Txt(text, style = MrType.label, color = color, maxLines = 1)
    }
}

@Composable
fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = Mr.colors
    val track by animateColorAsState(if (checked) colors.brand else colors.button, tween(150), label = "track")
    val knob by animateColorAsState(if (checked) colors.accentContrast else colors.base, tween(150), label = "knob")
    val offset by animateDpAsState(if (checked) 22.dp else 4.dp, spring(dampingRatio = 0.7f, stiffness = 800f), label = "offset")
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .background(track, CircleShape)
            .border(1.dp, if (checked) Color.Transparent else colors.surface5, CircleShape)
            .clip(CircleShape)
            .toggleable(checked, interactionSource = null, indication = null, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset { IntOffset(offset.roundToPx(), 0) }.size(20.dp).background(knob, CircleShape))
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Mr.colors.brand) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(400), label = "progress")
    Box(
        modifier
            .height(8.dp)
            .background(Mr.colors.button, CircleShape)
            .clip(CircleShape),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).background(color, CircleShape))
    }
}

/** Rounded square holding an icon, as used in front of Modrinth list items. */
@Composable
fun IconTile(icon: ImageVector, tint: Color, background: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier.size(size).background(background, RoundedCornerShape(MrSize.radiusMd)),
        contentAlignment = Alignment.Center,
    ) {
        MrIcon(icon, tint = tint, size = size * 0.5f)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Txt(text, modifier = modifier.padding(start = 4.dp), style = MrType.heading, color = Mr.colors.contrast)
}

@Composable
fun CoinsBadge(coins: Int, modifier: Modifier = Modifier) {
    Badge(
        text = coins.toString(),
        modifier = modifier,
        color = Mr.colors.orange,
        background = Mr.colors.highlight(Mr.colors.orange).copy(alpha = 0.16f),
        icon = io.github.dmrangel.wordlink.ui.icons.Lucide.Coins,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Mr.colors.button))
}

@Composable
fun SettingRow(title: String, modifier: Modifier = Modifier, description: String? = null, trailing: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = MrSize.gapMd)) {
            Txt(title, style = MrType.body, color = Mr.colors.contrast)
            if (description != null) Txt(description, style = MrType.small, color = Mr.colors.secondary)
        }
        trailing()
    }
}
