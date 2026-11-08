package io.github.dmrangel.wordlink.ui

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.ui.components.AppBackground
import io.github.dmrangel.wordlink.ui.game.GameScreen
import io.github.dmrangel.wordlink.ui.home.HomeScreen
import io.github.dmrangel.wordlink.ui.settings.SettingsScreen
import io.github.dmrangel.wordlink.ui.theme.WordlinkTheme
import java.util.Locale

/** Theme, interface language and sound effects around the app. */
@Composable
fun WordlinkRoot(vm: AppViewModel, fx: Fx, content: @Composable () -> Unit = { WordlinkApp(vm) }) {
    val settings = vm.settings
    LocalizedInterface(settings.language) {
        WordlinkTheme(settings.theme, settings.custom) {
            CompositionLocalProvider(LocalFx provides fx, content = content)
        }
    }
}

/**
 * The game language is also the interface language: strings, plurals and
 * dates resolve against it instead of the system locale.
 */
@Composable
private fun LocalizedInterface(language: Language, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val base = LocalConfiguration.current
    val locale = when (language) {
        Language.English -> Locale.US
        Language.Portuguese -> Locale.forLanguageTag("pt-BR")
    }
    val configuration = remember(base, locale) { Configuration(base).apply { setLocale(locale) } }
    // Wraps instead of replacing the Activity context, so lookups that walk the
    // context chain (photo picker, back dispatcher) still find the Activity.
    val localized = remember(context, configuration) {
        val resources = context.createConfigurationContext(configuration).resources
        object : ContextWrapper(context) {
            override fun getResources(): Resources = resources
        }
    }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides configuration,
        content = content,
    )
}

@Composable
fun WordlinkApp(vm: AppViewModel) {
    val session = vm.session
    Box(Modifier.fillMaxSize()) {
        AppBackground()
        AnimatedContent(
            targetState = if (vm.screen == Screen.Game && session == null) Screen.Home else vm.screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.98f)) togetherWith fadeOut(tween(120))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Game -> vm.session?.let { GameScreen(vm, it) }
            }
        }
    }
}
