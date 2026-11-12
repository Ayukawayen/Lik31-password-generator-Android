package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import net.ayukawayen.lik31.R

/**
 * Masked main key input plus the "save on this device" switch. The value is
 * passed on exactly as typed: the algorithm does not trim or normalize it.
 *
 * While a main key is saved and the field is empty, the saved key is in use;
 * typing replaces it for this session (and in storage once a password is copied).
 */
@Composable
fun MainKeyField(
    value: String,
    visible: Boolean,
    hasSavedKey: Boolean,
    saveEnabled: Boolean,
    saveFailed: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSaveToggle: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val usingSavedKey = hasSavedKey && value.isEmpty()
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = {
                Text(stringResource(if (usingSavedKey) R.string.main_key_saved else R.string.main_key))
            },
            placeholder = {
                if (usingSavedKey) Text(stringResource(R.string.main_key_saved_placeholder))
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            trailingIcon = {
                if (usingSavedKey) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = stringResource(R.string.main_key_is_saved),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                            contentDescription = stringResource(if (visible) R.string.hide_main_key else R.string.show_main_key),
                        )
                    }
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (saveFailed) R.string.save_failed else R.string.save_main_key),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (saveFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = saveEnabled,
                onCheckedChange = onSaveToggle,
            )
        }
    }
}
