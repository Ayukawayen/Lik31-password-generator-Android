package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ayukawayen.lik31.R

/**
 * Service key (wide) and the optional one-character symbol (narrow).
 * The keyboard's Done key on either field copies the password and closes.
 */
@Composable
fun ServiceKeyRow(
    serviceKey: String,
    symbol: String,
    serviceKeyFocus: FocusRequester,
    symbolFocus: FocusRequester,
    onServiceKeyChange: (String) -> Unit,
    onSymbolChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val mono = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = serviceKey,
            onValueChange = onServiceKeyChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(serviceKeyFocus),
            label = { Text(stringResource(R.string.service_key)) },
            singleLine = true,
            textStyle = mono,
            // A normal text keyboard (so non-Latin service names can be typed),
            // with auto-capitalization and auto-correction turned off.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        OutlinedTextField(
            value = symbol,
            onValueChange = onSymbolChange,
            modifier = Modifier
                .width(76.dp)
                .focusRequester(symbolFocus),
            label = { Text(stringResource(R.string.symbol)) },
            singleLine = true,
            textStyle = mono.copy(textAlign = TextAlign.Center),
            // Password keyboard type (shown in clear) = no suggestions, easy access to symbols.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
    }
}
