package io.github.dmrangel.wordlink.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.R
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.ui.AppViewModel
import io.github.dmrangel.wordlink.ui.Screen
import io.github.dmrangel.wordlink.ui.components.AppFrame
import io.github.dmrangel.wordlink.ui.components.ButtonLabel
import io.github.dmrangel.wordlink.ui.components.ButtonSize
import io.github.dmrangel.wordlink.ui.components.ButtonType
import io.github.dmrangel.wordlink.ui.components.Chip
import io.github.dmrangel.wordlink.ui.components.Divider
import io.github.dmrangel.wordlink.ui.components.Flow
import io.github.dmrangel.wordlink.ui.components.ModalLayer
import io.github.dmrangel.wordlink.ui.components.MrButton
import io.github.dmrangel.wordlink.ui.components.MrCard
import io.github.dmrangel.wordlink.ui.components.MrIcon
import io.github.dmrangel.wordlink.ui.components.NavButton
import io.github.dmrangel.wordlink.ui.components.SectionTitle
import io.github.dmrangel.wordlink.ui.components.SettingRow
import io.github.dmrangel.wordlink.ui.components.Toggle
import io.github.dmrangel.wordlink.ui.components.Txt
import io.github.dmrangel.wordlink.ui.home.AppLogo
import io.github.dmrangel.wordlink.ui.icons.Lucide
import io.github.dmrangel.wordlink.ui.theme.AccentPresets
import io.github.dmrangel.wordlink.ui.theme.CellShape
import io.github.dmrangel.wordlink.ui.theme.DarkColors
import io.github.dmrangel.wordlink.ui.theme.LightColors
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType
import io.github.dmrangel.wordlink.ui.theme.ThemeBasePresets
import io.github.dmrangel.wordlink.ui.theme.ThemeMode
import io.github.dmrangel.wordlink.ui.theme.toHex
import kotlin.math.roundToInt

private enum class Picker { Accent, ThemeBase }

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val colors = Mr.colors
    val settings = vm.settings
    val custom = settings.custom
    var picker by remember { mutableStateOf<Picker?>(null) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setBackground(uri)
    }
    BackHandler { if (picker != null) picker = null else vm.navigate(Screen.Home) }

    AppFrame(
        topBar = {
            NavButton(Lucide.ArrowLeft, onClick = { vm.navigate(Screen.Home) })
            Txt(stringResource(R.string.settings), Modifier.padding(start = MrSize.gapXs), style = MrType.title, color = colors.contrast)
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MrSize.gapLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(MrSize.gapLg)) {
                SectionTitle(stringResource(R.string.language))
                MrCard(Modifier.fillMaxWidth()) {
                    Flow(spacing = MrSize.gapSm) {
                        for (language in Language.entries) {
                            Chip(
                                language.nativeName,
                                selected = settings.language == language,
                                onClick = { vm.update { it.copy(language = language) } },
                                icon = Lucide.Languages,
                            )
                        }
                    }
                    Txt(stringResource(R.string.interface_language_desc), Modifier.padding(top = MrSize.gapSm), style = MrType.small, color = colors.secondary)
                }

                SectionTitle(stringResource(R.string.appearance))
                MrCard(Modifier.fillMaxWidth()) {
                    Txt(stringResource(R.string.theme), style = MrType.body, color = colors.contrast)
                    Flow(Modifier.padding(top = MrSize.gapMd), spacing = MrSize.gapSm) {
                        val options = listOf(
                            ThemeMode.Dark to R.string.theme_dark,
                            ThemeMode.Oled to R.string.theme_oled,
                            ThemeMode.Light to R.string.theme_light,
                            ThemeMode.System to R.string.theme_system,
                            ThemeMode.Custom to R.string.theme_custom,
                        )
                        for ((mode, label) in options) {
                            Chip(
                                stringResource(label),
                                selected = settings.theme == mode,
                                onClick = { vm.update { it.copy(theme = mode) } },
                                icon = when (mode) {
                                    ThemeMode.Light -> Lucide.Sun
                                    ThemeMode.System -> Lucide.Palette
                                    ThemeMode.Custom -> Lucide.Paintbrush
                                    else -> Lucide.Moon
                                },
                            )
                        }
                    }
                }

                SectionTitle(stringResource(R.string.customization))
                Column(verticalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                    Txt(stringResource(R.string.preview).uppercase(), Modifier.padding(start = 4.dp), style = MrType.caption, color = colors.secondary)
                    ThemePreview()
                }

                // Accent color
                MrCard(Modifier.fillMaxWidth()) {
                    Txt(stringResource(R.string.accent_color), style = MrType.body, color = colors.contrast)
                    Flow(Modifier.padding(top = MrSize.gapSm), spacing = MrSize.gapXs) {
                        val themeBrand = if (colors.isDark) DarkColors.brand else LightColors.brand
                        Swatch(
                            themeBrand,
                            selected = custom.accent == null,
                            onClick = { vm.customize { it.copy(accent = null) } },
                            description = stringResource(R.string.accent_default),
                            icon = Lucide.RotateCcw,
                        )
                        for (preset in AccentPresets.drop(1)) {
                            Swatch(
                                preset,
                                selected = custom.accent == preset.toArgb(),
                                onClick = { vm.customize { it.copy(accent = preset.toArgb()) } },
                                description = preset.toHex(),
                            )
                        }
                        val isCustom = custom.accent != null && AccentPresets.none { it.toArgb() == custom.accent }
                        Swatch(
                            if (isCustom) Color(custom.accent) else Color.Transparent,
                            selected = isCustom,
                            onClick = { picker = Picker.Accent },
                            description = stringResource(R.string.custom_color),
                            brush = if (isCustom) null else RainbowSweep,
                            icon = Lucide.Plus,
                        )
                    }
                }

                // Background photo
                MrCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Txt(stringResource(R.string.background_photo), style = MrType.body, color = colors.contrast)
                            Txt(stringResource(R.string.background_photo_desc), style = MrType.small, color = colors.secondary)
                        }
                    }
                    Row(Modifier.padding(top = MrSize.gapMd), horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                        val pick = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        MrButton(pick, Modifier.weight(1f), enabled = !vm.importingBackground) {
                            MrIcon(Lucide.Image, size = 18.dp)
                            ButtonLabel(
                                stringResource(
                                    when {
                                        vm.importingBackground -> R.string.loading
                                        custom.background == null -> R.string.choose_photo
                                        else -> R.string.change_photo
                                    },
                                ),
                            )
                        }
                        if (custom.background != null) {
                            MrButton(vm::clearBackground, color = colors.red, type = ButtonType.Outlined) {
                                MrIcon(Lucide.Trash, size = 18.dp)
                                ButtonLabel(stringResource(R.string.remove))
                            }
                        }
                    }
                    val enabled = custom.background != null
                    Column(Modifier.padding(top = MrSize.gapMd), verticalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                        LabeledSlider(
                            stringResource(R.string.blur),
                            custom.blur,
                            { v -> vm.customize(persist = false) { it.copy(blur = v) } },
                            onFinished = { vm.customize { it } },
                            enabled = enabled,
                        )
                        LabeledSlider(
                            stringResource(R.string.dim),
                            custom.dim / 0.85f,
                            { v -> vm.customize(persist = false) { it.copy(dim = v * 0.85f) } },
                            onFinished = { vm.customize { it } },
                            enabled = enabled,
                        )
                        LabeledSlider(
                            stringResource(R.string.panel_opacity),
                            (custom.panelOpacity - 0.3f) / 0.7f,
                            { v -> vm.customize(persist = false) { it.copy(panelOpacity = 0.3f + v * 0.7f) } },
                            onFinished = { vm.customize { it } },
                            enabled = enabled,
                            valueText = "${(custom.panelOpacity * 100).roundToInt()}%",
                        )
                    }
                }

                // Custom theme
                MrCard(Modifier.fillMaxWidth()) {
                    Txt(stringResource(R.string.custom_theme), style = MrType.body, color = colors.contrast)
                    Txt(stringResource(R.string.custom_theme_desc), style = MrType.small, color = colors.secondary)
                    Flow(Modifier.padding(top = MrSize.gapSm), spacing = MrSize.gapXs) {
                        val active = settings.theme == ThemeMode.Custom
                        for (preset in ThemeBasePresets) {
                            Swatch(
                                preset,
                                selected = active && custom.themeBase == preset.toArgb(),
                                onClick = { vm.update { it.copy(theme = ThemeMode.Custom, custom = it.custom.copy(themeBase = preset.toArgb())) } },
                                description = preset.toHex(),
                            )
                        }
                        val isCustom = ThemeBasePresets.none { it.toArgb() == custom.themeBase }
                        Swatch(
                            if (isCustom) Color(custom.themeBase) else Color.Transparent,
                            selected = active && isCustom,
                            onClick = { picker = Picker.ThemeBase },
                            description = stringResource(R.string.base_color),
                            brush = if (isCustom) null else RainbowSweep,
                            icon = Lucide.Plus,
                        )
                    }
                }

                // Board
                MrCard(Modifier.fillMaxWidth()) {
                    Txt(stringResource(R.string.board).uppercase(), Modifier.padding(bottom = MrSize.gapSm), style = MrType.caption, color = colors.secondary)
                    Txt(stringResource(R.string.tile_shape), style = MrType.body, color = colors.contrast)
                    Flow(Modifier.padding(top = MrSize.gapSm), spacing = MrSize.gapSm) {
                        val shapes = listOf(
                            Triple(CellShape.Square, R.string.shape_square, Lucide.Square),
                            Triple(CellShape.Rounded, R.string.shape_rounded, Lucide.Squircle),
                            Triple(CellShape.Circle, R.string.shape_circle, Lucide.Circle),
                        )
                        for ((shape, label, icon) in shapes) {
                            Chip(stringResource(label), custom.cellShape == shape, onClick = { vm.customize { it.copy(cellShape = shape) } }, icon = icon)
                        }
                    }
                    Divider(Modifier.padding(vertical = MrSize.gapMd))
                    LabeledSlider(
                        stringResource(R.string.wheel_size),
                        (custom.wheelScale - 0.7f) / 0.3f,
                        { v -> vm.customize(persist = false) { it.copy(wheelScale = 0.7f + v * 0.3f) } },
                        onFinished = { vm.customize { it } },
                        valueText = "${(custom.wheelScale * 100).roundToInt()}%",
                    )
                    Divider(Modifier.padding(vertical = MrSize.gapSm))
                    SettingRow(stringResource(R.string.left_handed), description = stringResource(R.string.left_handed_desc)) {
                        Toggle(custom.leftHanded, { on -> vm.customize { it.copy(leftHanded = on) } })
                    }
                    Divider(Modifier.padding(vertical = MrSize.gapSm))
                    SettingRow(stringResource(R.string.reduce_motion), description = stringResource(R.string.reduce_motion_desc)) {
                        Toggle(custom.reduceMotion, { on -> vm.customize { it.copy(reduceMotion = on) } })
                    }
                }

                MrButton(vm::resetCustomization, Modifier.fillMaxWidth(), size = ButtonSize.Xl) {
                    MrIcon(Lucide.RotateCcw, size = 20.dp)
                    ButtonLabel(stringResource(R.string.reset_customization), ButtonSize.Xl)
                }

                SectionTitle(stringResource(R.string.gameplay))
                MrCard(Modifier.fillMaxWidth()) {
                    SettingRow(stringResource(R.string.sound)) {
                        Toggle(settings.sound, { on -> vm.update { it.copy(sound = on) } })
                    }
                    Divider(Modifier.padding(vertical = MrSize.gapSm))
                    SettingRow(stringResource(R.string.haptics)) {
                        Toggle(settings.haptics, { on -> vm.update { it.copy(haptics = on) } })
                    }
                    Divider(Modifier.padding(vertical = MrSize.gapSm))
                    SettingRow(stringResource(R.string.high_refresh), description = stringResource(R.string.high_refresh_desc)) {
                        Toggle(settings.highRefreshRate, { on -> vm.update { it.copy(highRefreshRate = on) } })
                    }
                }

                SectionTitle(stringResource(R.string.about))
                MrCard(Modifier.fillMaxWidth()) {
                    val context = LocalContext.current
                    val version = remember {
                        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                        AppLogo(size = 44.dp)
                        Column {
                            Txt(stringResource(R.string.app_name), style = MrType.heading, color = colors.contrast)
                            Txt(stringResource(R.string.version, version), style = MrType.small, color = colors.secondary)
                        }
                    }
                    Txt(stringResource(R.string.about_text), Modifier.padding(top = MrSize.gapMd), style = MrType.body)
                    Txt(stringResource(R.string.credits), Modifier.padding(top = MrSize.gapSm), style = MrType.small, color = colors.secondary)
                }
            }
        }

        // Keeps the card on screen while the dialog fades out.
        var shown by remember { mutableStateOf<Picker?>(null) }
        if (picker != null) shown = picker
        ModalLayer(picker != null, onDismiss = { picker = null }) {
            Box {
                when (shown) {
                    Picker.Accent -> ColorPickerCard(
                        stringResource(R.string.pick_color),
                        initial = custom.accent?.let(::Color) ?: colors.brand,
                        presets = AccentPresets,
                        onApply = { color ->
                            vm.customize { it.copy(accent = color.toArgb()) }
                            picker = null
                        },
                        onCancel = { picker = null },
                    )
                    Picker.ThemeBase -> ColorPickerCard(
                        stringResource(R.string.base_color),
                        initial = Color(custom.themeBase),
                        presets = ThemeBasePresets,
                        onApply = { color ->
                            vm.update { it.copy(theme = ThemeMode.Custom, custom = it.custom.copy(themeBase = color.toArgb())) }
                            picker = null
                        },
                        onCancel = { picker = null },
                    )
                    null -> Unit
                }
            }
        }
    }
}
