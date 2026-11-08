package io.github.dmrangel.wordlink.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.dmrangel.wordlink.data.Backgrounds
import io.github.dmrangel.wordlink.ui.theme.LocalCustomization
import io.github.dmrangel.wordlink.ui.theme.Mr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The layer behind every screen: the theme's frame color, or the player's
 * photo with its blur and dim settings. The previous image stays on screen
 * while a new blur level is being prepared, so sliders don't flicker.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val colors = Mr.colors
    val custom = LocalCustomization.current
    val name = custom.background
    BoxWithConstraints(modifier.fillMaxSize().background(colors.raised)) {
        if (name == null) return@BoxWithConstraints
        val context = LocalContext.current
        val level = (custom.blur * 20).roundToInt()
        val width = constraints.maxWidth.coerceIn(360, 1440)
        var image by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(name, level, width) {
            image = withContext(Dispatchers.Default) {
                Backgrounds.load(context, name, level / 20f, width)?.asImageBitmap()
            }
        }
        image?.let {
            Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.High)
        }
        Box(Modifier.fillMaxSize().background((if (colors.isDark) Color.Black else Color.White).copy(alpha = custom.dim)))
    }
}
