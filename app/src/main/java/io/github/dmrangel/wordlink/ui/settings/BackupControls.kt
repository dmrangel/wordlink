package io.github.dmrangel.wordlink.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.dmrangel.wordlink.R
import io.github.dmrangel.wordlink.ui.components.ButtonLabel
import io.github.dmrangel.wordlink.ui.components.ButtonSize
import io.github.dmrangel.wordlink.ui.components.ButtonType
import io.github.dmrangel.wordlink.ui.components.MrButton
import io.github.dmrangel.wordlink.ui.components.MrCard
import io.github.dmrangel.wordlink.ui.components.MrIcon
import io.github.dmrangel.wordlink.ui.components.Txt
import io.github.dmrangel.wordlink.ui.icons.Lucide
import io.github.dmrangel.wordlink.ui.theme.Mr
import io.github.dmrangel.wordlink.ui.theme.MrSize
import io.github.dmrangel.wordlink.ui.theme.MrType

fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Wordlink", text))
}

private fun pastedText(context: Context): String? {
    val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return null
    return if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString() else null
}

/** Paste field for a backup code; [onRestore] returns false for an invalid code. */
@Composable
fun RestoreCard(onRestore: (String) -> Boolean, onCancel: () -> Unit) {
    val colors = Mr.colors
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(MrSize.radiusMd)

    MrCard(Modifier.fillMaxWidth(), padding = PaddingValues(MrSize.gapXl)) {
        Column(verticalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MrSize.gapMd)) {
                MrIcon(Lucide.ClipboardPaste, tint = colors.brand, size = 22.dp)
                Txt(stringResource(R.string.restore_title), Modifier.weight(1f), style = MrType.title, color = colors.contrast)
            }
            Txt(stringResource(R.string.restore_desc), style = MrType.small, color = colors.secondary)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .background(colors.surface2, shape)
                    .border(1.dp, if (invalid) colors.red else colors.surface5, shape)
                    .padding(MrSize.gapMd),
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        invalid = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("backup-code"),
                    textStyle = MrType.small.copy(color = colors.contrast),
                    cursorBrush = SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
            if (invalid) Txt(stringResource(R.string.invalid_code), style = MrType.small, color = colors.red)
            MrButton(
                onClick = {
                    pastedText(context)?.let {
                        code = it.trim()
                        invalid = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                type = ButtonType.Outlined,
                color = colors.brand,
            ) {
                MrIcon(Lucide.ClipboardPaste, size = 18.dp)
                ButtonLabel(stringResource(R.string.paste))
            }
            Row(Modifier.padding(top = MrSize.gapSm), horizontalArrangement = Arrangement.spacedBy(MrSize.gapSm)) {
                MrButton(onCancel, Modifier.weight(1f), size = ButtonSize.Xl) {
                    ButtonLabel(stringResource(R.string.cancel), ButtonSize.Xl)
                }
                MrButton(
                    onClick = { invalid = !onRestore(code) },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.Colored,
                    size = ButtonSize.Xl,
                    enabled = code.isNotBlank(),
                ) {
                    ButtonLabel(stringResource(R.string.restore), ButtonSize.Xl)
                }
            }
        }
    }
}
