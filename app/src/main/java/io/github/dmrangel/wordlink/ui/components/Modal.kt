package io.github.dmrangel.wordlink.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.ui.theme.MrSize

/** Dimmed backdrop with a centered card, like Modrinth's NewModal. */
@Composable
fun ModalLayer(visible: Boolean, onDismiss: (() -> Unit)?, content: @Composable BoxScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = onDismiss != null) { onDismiss?.invoke() }
                .padding(MrSize.gapLg),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .animateEnterExit(
                        enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = 500f), initialScale = 0.85f),
                        exit = scaleOut(tween(120), targetScale = 0.95f),
                    )
                    .widthIn(max = 420.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                content = content,
            )
        }
    }
}
