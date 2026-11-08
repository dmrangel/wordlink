package io.github.dmrangel.wordlink.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.dmrangel.wordlink.R
import io.github.dmrangel.wordlink.game.Feedback
import io.github.dmrangel.wordlink.game.GameSession
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.game.LevelId
import io.github.dmrangel.wordlink.game.date
import io.github.dmrangel.wordlink.game.WordResult
import io.github.dmrangel.wordlink.ui.AppViewModel
import io.github.dmrangel.wordlink.ui.LocalFx
import io.github.dmrangel.wordlink.ui.Screen
import io.github.dmrangel.wordlink.ui.components.AppFrame
import io.github.dmrangel.wordlink.ui.components.ButtonSize
import io.github.dmrangel.wordlink.ui.components.ButtonType
import io.github.dmrangel.wordlink.ui.components.CoinsBadge
import io.github.dmrangel.wordlink.ui.components.MrButton
import io.github.dmrangel.wordlink.ui.components.ModalLayer
import io.github.dmrangel.wordlink.ui.components.MrIcon
import io.github.dmrangel.wordlink.ui.components.NavButton
import io.github.dmrangel.wordlink.ui.components.Txt
import io.github.dmrangel.wordlink.ui.icons.Lucide
import io.github.dmrangel.wordlink.ui.theme.LocalCustomization
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GameScreen(vm: AppViewModel, session: GameSession) {
    val fx = LocalFx.current
    var current by remember(session) { mutableStateOf("") }
    var showBonus by remember(session) { mutableStateOf(false) }
    var showCompletion by remember(session) { mutableStateOf(false) }
    val completion = vm.completion?.takeIf { it.id == session.level.id }

    BackHandler {
        when {
            showBonus -> showBonus = false
            session.targeting -> session.targeting = false
            else -> vm.navigate(Screen.Home)
        }
    }

    // Let the last word's reveal play out before the results card appears.
    LaunchedEffect(completion) {
        if (completion != null) {
            fx.complete()
            delay(650)
            showCompletion = true
        }
    }

    AppFrame(
        topBar = {
            NavButton(Lucide.ArrowLeft, onClick = { vm.navigate(Screen.Home) })
            Column(Modifier.weight(1f).padding(start = MrSize.gapXs)) {
                val id = session.level.id
                Txt(
                    when (id) {
                        is LevelId.Campaign -> stringResource(R.string.level_n, id.number)
                        is LevelId.Daily -> stringResource(
                            R.string.daily_title,
                            id.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0])),
                        )
                    },
                    style = MrType.heading,
                    color = Mr.colors.contrast,
                    maxLines = 1,
                )
                Txt(id.language.nativeName, style = MrType.small, color = Mr.colors.secondary)
            }
            CoinsBadge(vm.profile.coins, Modifier.padding(end = MrSize.gapSm))
        },
    ) {
        val enabled = !session.isSolved
        val onSubmit: (String) -> Unit = { word ->
            current = ""
            when (session.submit(word)) {
                WordResult.Found -> fx.word()
                WordResult.Bonus -> fx.bonus()
                WordResult.Repeat, WordResult.BonusRepeat -> fx.tick()
                WordResult.Invalid -> if (word.length >= 2) fx.error()
            }
        }
        val wheel: @Composable (Modifier) -> Unit = { modifier ->
            LetterWheel(
                letters = session.level.letters,
                order = session.order,
                enabled = enabled && !session.targeting,
                onChange = { current = it },
                onLetter = fx::letter,
                onSubmit = onSubmit,
                modifier = modifier.testTag("wheel"),
            )
        }
        val controls: @Composable (Dp) -> Unit = { size ->
            Controls(
                session = session,
                jar = vm.profile.jar,
                buttonSize = size,
                onShuffle = {
                    session.shuffle()
                    fx.shuffle()
                },
                onBonus = { showBonus = true },
                onHint = { session.hint(AppViewModel.HINT_COST) },
                onTarget = { session.targeting = !session.targeting },
                wheel = wheel,
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize().padding(MrSize.gapLg), horizontalArrangement = Arrangement.spacedBy(MrSize.gapLg)) {
                    CrosswordGrid(session, onTarget = { session.target(it, AppViewModel.TARGET_COST) }, Modifier.weight(1.15f).fillMaxHeight())
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        WordPreview(current, session.feedback, session.targeting, Modifier.fillMaxWidth().height(56.dp))
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { controls(52.dp) }
                    }
                }
            } else {
                val wheelArea = min(maxWidth * 0.86f, maxHeight * 0.44f)
                Column(Modifier.fillMaxSize().padding(horizontal = MrSize.gapLg), horizontalAlignment = Alignment.CenterHorizontally) {
                    CrosswordGrid(
                        session,
                        onTarget = { session.target(it, AppViewModel.TARGET_COST) },
                        Modifier.weight(1f).fillMaxWidth().padding(top = MrSize.gapLg, bottom = MrSize.gapSm),
                    )
                    WordPreview(current, session.feedback, session.targeting, Modifier.fillMaxWidth().height(56.dp))
                    Box(Modifier.fillMaxWidth().height(wheelArea).padding(bottom = MrSize.gapSm)) { controls(52.dp) }
                }
            }
        }

        ToastHost(vm.toast, vm::dismissToast, Modifier.align(Alignment.TopCenter))

        ModalLayer(showBonus, onDismiss = { showBonus = false }) {
            BonusCard(
                session.bonus.map(session::display).sorted(),
                vm.profile.jar,
                rule = if (session.level.id.language == Language.Portuguese) stringResource(R.string.bonus_rule_pt) else null,
                onClose = { showBonus = false },
            )
        }
        if (showCompletion && completion != null && !LocalCustomization.current.reduceMotion) Confetti(Modifier.fillMaxSize())
        ModalLayer(showCompletion && completion != null, onDismiss = null) {
            val shown = completion ?: return@ModalLayer
            CompletionCard(
                shown,
                onNext = if (shown.id is LevelId.Campaign) ({ vm.play() }) else null,
                onHome = { vm.navigate(Screen.Home) },
            )
        }
    }
}

/** The wheel with the four round action buttons in the corners around it. */
@Composable
private fun Controls(
    session: GameSession,
    jar: Int,
    buttonSize: Dp,
    onShuffle: () -> Unit,
    onBonus: () -> Unit,
    onHint: () -> Unit,
    onTarget: () -> Unit,
    wheel: @Composable (Modifier) -> Unit,
) {
    val colors = Mr.colors
    val custom = LocalCustomization.current
    // Shuffle and bonus sit on the left, hint and target on the right; swapped for left-handed play.
    val (playSide, helpSide) = if (custom.leftHanded) Alignment.CenterEnd to Alignment.CenterStart else Alignment.CenterStart to Alignment.CenterEnd
    BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = 8.dp)) {
        val diameter = min(maxHeight, maxWidth - buttonSize * 2 + 24.dp) * custom.wheelScale
        wheel(Modifier.align(Alignment.Center).size(diameter).aspectRatio(1f))

        Column(
            Modifier.align(playSide).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (custom.leftHanded) Alignment.End else Alignment.Start,
        ) {
            RoundAction(Lucide.Shuffle, stringResource(R.string.shuffle), buttonSize, onShuffle)
            Box {
                val bump = remember { Animatable(1f) }
                LaunchedEffect(session.bonus.size) {
                    if (session.bonus.isNotEmpty()) {
                        bump.animateTo(1.25f, tween(120))
                        bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
                    }
                }
                RoundAction(
                    Lucide.Sparkles,
                    stringResource(R.string.bonus),
                    buttonSize,
                    onBonus,
                    Modifier.graphicsLayer {
                        scaleX = bump.value
                        scaleY = bump.value
                    },
                    tint = colors.blue,
                )
                if (jar > 0) CountTag(jar, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
            }
        }
        Column(
            Modifier.align(helpSide).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (custom.leftHanded) Alignment.Start else Alignment.End,
        ) {
            Box {
                RoundAction(Lucide.Lightbulb, stringResource(R.string.hint), buttonSize, onHint, tint = colors.orange)
                CostTag(AppViewModel.HINT_COST, Modifier.align(Alignment.BottomCenter).offset(y = 6.dp))
            }
            Box {
                RoundAction(
                    Lucide.Target,
                    stringResource(R.string.target),
                    buttonSize,
                    onTarget,
                    active = session.targeting,
                    tint = if (session.targeting) colors.accentContrast else colors.red,
                )
                CostTag(AppViewModel.TARGET_COST, Modifier.align(Alignment.BottomCenter).offset(y = 6.dp))
            }
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = Color.Unspecified,
) {
    MrButton(
        onClick,
        modifier.size(size).semantics { contentDescription = label },
        type = if (active) ButtonType.Colored else ButtonType.Base,
        size = ButtonSize.Xxl,
        circular = true,
        iconOnly = true,
    ) {
        MrIcon(icon, tint = tint, size = 24.dp)
    }
}

/** The pill above the wheel: the word being spelled, then the verdict. */
@Composable
private fun WordPreview(current: String, feedback: Feedback?, targeting: Boolean, modifier: Modifier) {
    val colors = Mr.colors
    var shown by remember { mutableStateOf<Feedback?>(null) }
    val shake = remember { Animatable(0f) }
    val reduceMotion = LocalCustomization.current.reduceMotion
    LaunchedEffect(feedback?.id) {
        val next = feedback ?: return@LaunchedEffect
        shown = next
        if (next.result == WordResult.Invalid && !reduceMotion) {
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                -14f at 50
                12f at 110
                -9f at 170
                6f at 230
                -3f at 290
            })
        }
        delay(900)
        if (shown?.id == next.id) shown = null
    }

    val verdict = shown?.takeIf { current.isEmpty() }
    val text = when {
        current.isNotEmpty() -> current.uppercase()
        verdict != null -> verdict.text.uppercase()
        targeting -> stringResource(R.string.target_prompt)
        else -> ""
    }
    val background = when {
        current.isNotEmpty() -> colors.button
        verdict != null -> when (verdict.result) {
            WordResult.Found -> colors.brand
            WordResult.Bonus -> colors.blue
            WordResult.Repeat, WordResult.BonusRepeat -> colors.orange
            WordResult.Invalid -> colors.red
        }
        targeting -> colors.brandHighlight
        else -> Color.Transparent
    }
    val textColor = when {
        current.isNotEmpty() -> colors.contrast
        verdict != null -> colors.accentContrast
        else -> colors.brand
    }
    val alpha by animateFloatAsState(if (text.isEmpty()) 0f else 1f, tween(140), label = "preview")
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    translationX = shake.value * density
                }
                .background(background, CircleShape)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm),
        ) {
            if (verdict?.result == WordResult.Bonus) MrIcon(Lucide.Sparkles, tint = textColor, size = 18.dp)
            Txt(
                text,
                style = if (targeting && current.isEmpty() && verdict == null) MrType.label else MrType.title.copy(letterSpacing = MrType.caption.letterSpacing * 3),
                color = textColor,
                maxLines = 1,
            )
        }
    }
}
