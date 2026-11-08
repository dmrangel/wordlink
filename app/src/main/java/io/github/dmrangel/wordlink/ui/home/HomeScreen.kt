package io.github.dmrangel.wordlink.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.R
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.ui.AppViewModel
import io.github.dmrangel.wordlink.ui.Screen
import io.github.dmrangel.wordlink.ui.components.AppFrame
import io.github.dmrangel.wordlink.ui.components.Badge
import io.github.dmrangel.wordlink.ui.components.ButtonLabel
import io.github.dmrangel.wordlink.ui.components.ButtonSize
import io.github.dmrangel.wordlink.ui.components.ButtonType
import io.github.dmrangel.wordlink.ui.components.CoinsBadge
import io.github.dmrangel.wordlink.ui.components.IconTile
import io.github.dmrangel.wordlink.ui.components.MrButton
import io.github.dmrangel.wordlink.ui.components.MrCard
import io.github.dmrangel.wordlink.ui.components.MrIcon
import io.github.dmrangel.wordlink.ui.components.NavButton
import io.github.dmrangel.wordlink.ui.components.ProgressBar
import io.github.dmrangel.wordlink.ui.components.SectionTitle
import io.github.dmrangel.wordlink.ui.components.Txt
import io.github.dmrangel.wordlink.ui.icons.Lucide
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType

private const val MILESTONE = 10

private val LogoPoints = listOf(0.22f to 0.32f, 0.36f to 0.70f, 0.50f to 0.42f, 0.64f to 0.70f, 0.78f to 0.32f)

@Composable
fun HomeScreen(vm: AppViewModel) {
    val colors = Mr.colors
    val profile = vm.profile
    val language = vm.settings.language
    val level = profile.level(language)
    val today = vm.today
    val dailySolved = profile.dailySolved(language, today)
    val streak = if (profile.lastDaily != null && !profile.lastDaily.isBefore(today.minusDays(1))) profile.streak else 0

    AppFrame(
        topBar = {
            AppLogo(Modifier.padding(start = MrSize.gapSm), size = 36.dp)
            Txt(stringResource(R.string.app_name), Modifier.padding(start = MrSize.gapMd).weight(1f), style = MrType.title, color = colors.contrast)
            CoinsBadge(profile.coins)
            NavButton(Lucide.Settings, onClick = { vm.navigate(Screen.Settings) })
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MrSize.gapLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(MrSize.gapLg)) {
                // Hero: current level and the big play button.
                MrCard(Modifier.fillMaxWidth(), background = Brush.horizontalGradient(colors.brandGradient)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Txt(stringResource(R.string.level_caption).uppercase(), style = MrType.caption, color = colors.brand)
                            Txt(level.toString(), style = MrType.display, color = colors.contrast)
                        }
                        Badge(language.nativeName, icon = Lucide.Languages, color = colors.base, background = colors.button)
                    }
                    val inMilestone = (level - 1) % MILESTONE
                    ProgressBar(inMilestone / MILESTONE.toFloat(), Modifier.fillMaxWidth().padding(top = MrSize.gapMd))
                    Txt(
                        pluralStringResource(R.plurals.next_milestone, MILESTONE - inMilestone, MILESTONE - inMilestone),
                        Modifier.padding(top = MrSize.gapSm),
                        style = MrType.small,
                        color = colors.secondary,
                    )
                    MrButton(
                        onClick = { vm.play() },
                        modifier = Modifier.fillMaxWidth().padding(top = MrSize.gapLg),
                        type = ButtonType.Colored,
                        size = ButtonSize.Xxl,
                        enabled = !vm.loading,
                    ) {
                        MrIcon(Lucide.Play, size = 22.dp)
                        ButtonLabel(stringResource(if (vm.loading) R.string.loading else R.string.play), ButtonSize.Xl)
                    }
                }

                SectionTitle(stringResource(R.string.language))
                Row(horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                    for (option in Language.entries) {
                        LanguageCard(
                            language = option,
                            level = profile.level(option),
                            selected = option == language,
                            onClick = { vm.update { it.copy(language = option) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                MrCard(Modifier.fillMaxWidth(), onClick = { vm.play(daily = true) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                        IconTile(Lucide.CalendarDays, tint = colors.blue, background = colors.highlight(colors.blue))
                        Column(Modifier.weight(1f)) {
                            Txt(stringResource(R.string.daily_puzzle), style = MrType.heading, color = colors.contrast)
                            Txt(
                                stringResource(if (dailySolved) R.string.daily_solved else R.string.daily_new),
                                style = MrType.small,
                                color = colors.secondary,
                            )
                        }
                        if (dailySolved) {
                            IconTile(Lucide.Check, tint = colors.brand, background = colors.brandHighlight, size = 36.dp)
                        } else {
                            MrIcon(Lucide.ChevronRight, tint = colors.secondary)
                        }
                    }
                    if (streak > 0) {
                        Badge(
                            pluralStringResource(R.plurals.days, streak, streak),
                            Modifier.padding(top = MrSize.gapMd),
                            icon = Lucide.Flame,
                            color = colors.orange,
                            background = colors.highlight(colors.orange),
                        )
                    }
                }

                SectionTitle(stringResource(R.string.stats))
                val completed = profile.levels.values.sumOf { it - 1 }
                Row(horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                    StatCard(Lucide.Trophy, colors.brand, completed, stringResource(R.string.stat_levels), Modifier.weight(1f))
                    StatCard(Lucide.BookOpen, colors.purple, profile.wordsFound, stringResource(R.string.stat_words), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                    StatCard(Lucide.Sparkles, colors.blue, profile.bonusFound, stringResource(R.string.stat_bonus), Modifier.weight(1f))
                    StatCard(Lucide.Flame, colors.orange, streak, stringResource(R.string.stat_streak), Modifier.weight(1f))
                }
                Spacer(Modifier.size(MrSize.gapSm))
            }
        }
    }
}

@Composable
private fun LanguageCard(language: Language, level: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = Mr.colors
    val shape = RoundedCornerShape(MrSize.radiusLg)
    MrCard(
        modifier.border(2.dp, if (selected) colors.brand else Color.Transparent, shape),
        background = if (selected) Brush.linearGradient(listOf(colors.brandHighlight.copy(alpha = 0.18f), Color.Transparent)) else null,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(if (selected) colors.brand else colors.button, RoundedCornerShape(MrSize.radiusMd)),
                contentAlignment = Alignment.Center,
            ) {
                Txt(language.code.uppercase(), style = MrType.label, color = if (selected) colors.accentContrast else colors.contrast)
            }
            Spacer(Modifier.weight(1f))
            if (selected) MrIcon(Lucide.Check, tint = colors.brand)
        }
        Txt(language.nativeName, Modifier.padding(top = MrSize.gapMd), style = MrType.heading, color = colors.contrast, maxLines = 1)
        Txt(stringResource(R.string.level_n, level), style = MrType.small, color = colors.secondary)
    }
}

@Composable
private fun StatCard(icon: ImageVector, tint: Color, value: Int, label: String, modifier: Modifier) {
    val colors = Mr.colors
    MrCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
            IconTile(icon, tint = tint, background = colors.highlight(tint), size = 40.dp)
            Column {
                Txt(value.toString(), style = MrType.heading, color = colors.contrast, maxLines = 1)
                Txt(label, style = MrType.small, color = colors.secondary, maxLines = 1)
            }
        }
    }
}

/** Brand tile with a "W" traced through five linked letters, like the launcher icon. */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val colors = Mr.colors
    Canvas(modifier.size(size)) {
        val s = this.size.width
        drawRoundRect(colors.brand, cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.28f))
        val ink = colors.accentContrast
        val points = LogoPoints.map { (x, y) -> Offset(s * x, s * y) }
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (p in points.drop(1)) lineTo(p.x, p.y)
        }
        drawPath(path, ink, style = Stroke(s * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        for (p in points) drawCircle(ink, s * 0.075f, p)
    }
}
