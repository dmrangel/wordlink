package io.github.dmrangel.wordlink.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.R
import io.github.dmrangel.wordlink.game.LevelId
import io.github.dmrangel.wordlink.ui.AppViewModel
import io.github.dmrangel.wordlink.ui.Completion
import io.github.dmrangel.wordlink.ui.Toast
import io.github.dmrangel.wordlink.ui.components.Badge
import io.github.dmrangel.wordlink.ui.components.ButtonLabel
import io.github.dmrangel.wordlink.ui.components.ButtonSize
import io.github.dmrangel.wordlink.ui.components.ButtonType
import io.github.dmrangel.wordlink.ui.components.Flow
import io.github.dmrangel.wordlink.ui.components.IconTile
import io.github.dmrangel.wordlink.ui.components.MrButton
import io.github.dmrangel.wordlink.ui.components.MrCard
import io.github.dmrangel.wordlink.ui.components.MrIcon
import io.github.dmrangel.wordlink.ui.components.ProgressBar
import io.github.dmrangel.wordlink.ui.components.Txt
import io.github.dmrangel.wordlink.ui.icons.Lucide
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CompletionCard(completion: Completion, onNext: (() -> Unit)?, onHome: () -> Unit) {
    val colors = Mr.colors
    MrCard(Modifier.fillMaxWidth(), padding = PaddingValues(MrSize.gapXl)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
            IconTile(Lucide.Trophy, tint = colors.brand, background = colors.brandHighlight, size = 64.dp)
            Txt(
                stringResource(if (completion.id is LevelId.Daily) R.string.daily_complete else R.string.level_complete),
                style = MrType.title,
                color = colors.contrast,
                align = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                Badge(pluralStringResource(R.plurals.words_found, completion.words, completion.words), icon = Lucide.Check, color = colors.brand, background = colors.brandHighlight)
                if (completion.bonus > 0) {
                    Badge(pluralStringResource(R.plurals.bonus_found, completion.bonus, completion.bonus), icon = Lucide.Sparkles, color = colors.blue, background = colors.highlight(colors.blue))
                }
            }
            if (completion.reward > 0) {
                Badge("+${completion.reward}", icon = Lucide.Coins, color = colors.orange, background = colors.highlight(colors.orange))
            }
            Column(Modifier.fillMaxWidth().padding(top = MrSize.gapSm), verticalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                if (onNext != null) {
                    MrButton(onNext, Modifier.fillMaxWidth(), type = ButtonType.Colored, size = ButtonSize.Xl) {
                        ButtonLabel(stringResource(R.string.next_level), ButtonSize.Xl)
                        MrIcon(Lucide.ChevronRight)
                    }
                }
                MrButton(onHome, Modifier.fillMaxWidth(), type = if (onNext == null) ButtonType.Colored else ButtonType.Base, size = ButtonSize.Xl) {
                    MrIcon(Lucide.House)
                    ButtonLabel(stringResource(R.string.home), ButtonSize.Xl)
                }
            }
        }
    }
}

@Composable
fun BonusCard(words: List<String>, jar: Int, rule: String?, onClose: () -> Unit) {
    val colors = Mr.colors
    MrCard(Modifier.fillMaxWidth(), padding = PaddingValues(MrSize.gapXl)) {
        Column(verticalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                IconTile(Lucide.Sparkles, tint = colors.blue, background = colors.highlight(colors.blue))
                Txt(stringResource(R.string.bonus_title), Modifier.weight(1f), style = MrType.title, color = colors.contrast)
            }
            ProgressBar(jar / AppViewModel.JAR_SIZE.toFloat(), Modifier.fillMaxWidth(), color = colors.blue)
            Txt(
                stringResource(R.string.jar_progress, jar, AppViewModel.JAR_SIZE, AppViewModel.JAR_REWARD),
                style = MrType.small,
                color = colors.secondary,
            )
            if (rule != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                    MrIcon(Lucide.BookOpen, tint = colors.secondary, size = 16.dp)
                    Txt(rule, Modifier.weight(1f), style = MrType.small, color = colors.secondary)
                }
            }
            if (words.isEmpty()) {
                Txt(stringResource(R.string.bonus_empty), style = MrType.body, color = colors.base)
            } else {
                Box(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Flow(spacing = MrSize.gapSm) {
                        for (word in words) Badge(word.uppercase(), color = colors.contrast)
                    }
                }
            }
            MrButton(onClose, Modifier.fillMaxWidth(), size = ButtonSize.Xl) {
                ButtonLabel(stringResource(R.string.close), ButtonSize.Xl)
            }
        }
    }
}

/** Notification pill that drops in from the top of the board. */
@Composable
fun ToastHost(toast: Toast?, onDismiss: (Long) -> Unit, modifier: Modifier = Modifier) {
    val colors = Mr.colors
    LaunchedEffect(toast?.id) {
        val id = toast?.id ?: return@LaunchedEffect
        delay(2000)
        onDismiss(id)
    }
    val last = remember { mutableStateOf(toast) }
    if (toast != null) last.value = toast
    AnimatedVisibility(
        toast != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 600f)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val current = last.value ?: return@AnimatedVisibility
        val (icon, tint, text) = when (current.kind) {
            Toast.Kind.JarReward -> Triple(Lucide.Coins, colors.orange, stringResource(R.string.jar_reward, current.amount))
            Toast.Kind.NotEnoughCoins -> Triple(Lucide.X, colors.red, stringResource(R.string.not_enough_coins))
        }
        Row(
            Modifier
                .padding(top = MrSize.gapMd)
                .background(colors.button, CircleShape)
                .padding(horizontal = MrSize.gapLg, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm),
        ) {
            MrIcon(icon, tint = tint, size = 18.dp)
            Txt(text, style = MrType.label, color = colors.contrast)
        }
    }
}

private class Particle(val angle: Float, val speed: Float, val spin: Float, val color: Int, val size: Float, val drift: Float)

/** A short burst of confetti; one Canvas, drawn from a single animated clock. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val colors = Mr.colors
    val palette = listOf(colors.brand, colors.blue, colors.purple, colors.orange, colors.red)
    val particles = remember {
        val random = Random(System.nanoTime())
        List(80) {
            Particle(
                angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 0.9).toFloat(),
                speed = 0.55f + random.nextFloat() * 0.65f,
                spin = (random.nextFloat() - 0.5f) * 1080f,
                color = random.nextInt(5),
                size = 6f + random.nextFloat() * 6f,
                drift = (random.nextFloat() - 0.5f) * 0.3f,
            )
        }
    }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(1f, tween(2200, easing = LinearEasing)) }
    Canvas(modifier) {
        val t = clock.value
        if (t >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height * 0.42f)
        val reach = size.height * 0.9f
        for (p in particles) {
            val x = origin.x + (cos(p.angle) * p.speed + p.drift * t) * reach * t
            val y = origin.y + sin(p.angle) * p.speed * reach * t + 1.1f * reach * t * t
            val w = p.size * density
            rotate(p.spin * t, Offset(x, y)) {
                drawRoundRect(
                    palette[p.color].copy(alpha = (1f - t * t).coerceIn(0f, 1f)),
                    topLeft = Offset(x - w / 2, y - w / 4),
                    size = Size(w, w / 2),
                    cornerRadius = CornerRadius(w / 6),
                )
            }
        }
    }
}

@Composable
fun CostTag(cost: Int, modifier: Modifier = Modifier) {
    val colors = Mr.colors
    Row(
        modifier
            .background(colors.surface5, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MrIcon(Lucide.Coins, tint = colors.orange, size = 11.dp)
        Txt(cost.toString(), style = MrType.caption.copy(letterSpacing = MrType.small.letterSpacing), color = colors.contrast)
    }
}

@Composable
fun CountTag(count: Int, modifier: Modifier = Modifier) {
    val colors = Mr.colors
    Box(
        modifier
            .size(20.dp)
            .background(colors.blue, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Txt(count.toString(), style = MrType.caption.copy(letterSpacing = MrType.small.letterSpacing), color = colors.accentContrast)
    }
}
