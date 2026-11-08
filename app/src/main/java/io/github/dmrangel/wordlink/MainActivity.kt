package io.github.dmrangel.wordlink

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dmrangel.wordlink.ui.AppViewModel
import io.github.dmrangel.wordlink.ui.Fx
import io.github.dmrangel.wordlink.ui.WordlinkApp
import io.github.dmrangel.wordlink.ui.WordlinkRoot
import io.github.dmrangel.wordlink.ui.theme.Mr

class MainActivity : ComponentActivity() {
    private lateinit var fx: Fx

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        fx = Fx(this)

        setContent {
            val vm: AppViewModel = viewModel()
            val settings = vm.settings
            LaunchedEffect(settings.highRefreshRate) { preferRefreshRate(highest = settings.highRefreshRate) }
            val view = LocalView.current
            SideEffect {
                fx.view = view
                fx.sound = settings.sound
                fx.haptics = settings.haptics
            }
            WordlinkRoot(vm, fx) {
                val dark = Mr.colors.isDark
                DisposableEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose {}
                }
                WordlinkApp(vm)
            }
        }
    }

    /**
     * Many phones keep apps at 60 Hz unless they ask for more. Picks the
     * fastest mode at the current resolution (or the default one when off),
     * so drags and animations can run at 90, 120 or 144 Hz.
     */
    private fun preferRefreshRate(highest: Boolean) {
        val display: Display = (if (Build.VERSION.SDK_INT >= 30) display else null)
            ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
        val current = display.mode
        val mode = if (highest) {
            display.supportedModes
                .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
                .maxByOrNull { it.refreshRate }
        } else {
            null
        }
        window.attributes = window.attributes.also { it.preferredDisplayModeId = mode?.modeId ?: 0 }
    }

    override fun onDestroy() {
        fx.release()
        super.onDestroy()
    }
}
