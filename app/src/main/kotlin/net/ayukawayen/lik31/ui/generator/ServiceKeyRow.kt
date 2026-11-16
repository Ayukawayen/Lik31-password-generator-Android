package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
 * An icon in the service key field shows that length and symbol are the ones
 * remembered for this service (an icon, so the layout height never changes).
 * Likewise a whitespace warning is shown by recolouring the label and border.
 */
@Composable
fun ServiceKeyRow(
    serviceKey: String,
    symbol: String,
    configApplied: Boolean,
    edgeWhitespace: Boolean,
    serviceKeyFocus: FocusRequester,
    symbolFocus: FocusRequester,
    onServiceKeyChange: (String) -> Unit,
    onSymbolChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val mono = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
    val accent = MaterialTheme.colorScheme.tertiary
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
            // Leading/trailing whitespace changes the password: flag it, never block it.
            label = {
                Text(
                    text = stringResource(if (edgeWhitespace) R.string.service_key_whitespace else R.string.service_key),
                    color = if (edgeWhitespace) accent else Color.Unspecified,
                    maxLines = 1,
                )
            },
            colors = if (edgeWhitespace) {
                OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent)
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            singleLine = true,
            textStyle = mono,
            trailingIcon = if (configApplied) {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = stringResource(R.string.config_applied),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
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
            label = { Text(stringResource(R.string.symbol), maxLines = 1) },
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
