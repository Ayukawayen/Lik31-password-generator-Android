package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import net.ayukawayen.lik31.R

/**
 * Masked main key input. The "save on this device" toggle lives inside the
 * field as an icon button. The value is passed on exactly as typed: the
 * algorithm does not trim or normalize the main key.
 *
 * A saved main key exists only as a hash, so while it is in use
 * ([MainKeyMode.UsingSaved]) the field stays empty, shows no dots (the length
 * is unknown) and offers no eye button.
 */
@Composable
fun MainKeyField(
    value: String,
    mode: MainKeyMode,
    visible: Boolean,
    canReveal: Boolean,
    saveEnabled: Boolean,
    savePending: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSaveToggle: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val replacing = mode == MainKeyMode.ReplacingSaved
    val accent = MaterialTheme.colorScheme.tertiary
    val labelRes = when (mode) {
        MainKeyMode.Normal -> R.string.main_key
        MainKeyMode.UsingSaved, MainKeyMode.RetypedSaved -> R.string.main_key_saved
        MainKeyMode.ReplacingSaved -> R.string.main_key_replacing
        MainKeyMode.SaveFailed -> R.string.main_key_save_failed
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        label = {
            Text(
                text = stringResource(labelRes),
                color = if (replacing) accent else Color.Unspecified,
                maxLines = 1,
            )
        },
        placeholder = {
            if (mode == MainKeyMode.UsingSaved) Text(stringResource(R.string.main_key_saved_placeholder), maxLines = 1)
        },
        isError = mode == MainKeyMode.SaveFailed,
        colors = if (replacing) {
            OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = accent)
        } else {
            OutlinedTextFieldDefaults.colors()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canReveal) {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                            contentDescription = stringResource(if (visible) R.string.hide_main_key else R.string.show_main_key),
                        )
                    }
                }
                SaveToggle(
                    checked = saveEnabled,
                    pending = savePending,
                    failed = mode == MainKeyMode.SaveFailed,
                    onCheckedChange = onSaveToggle,
                )
            }
        },
    )
}

/** Icon toggle for "save main key on this device", with a dot while a save is pending. */
@Composable
private fun SaveToggle(
    checked: Boolean,
    pending: Boolean,
    failed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val pendingText = stringResource(R.string.save_pending)
    BadgedBox(
        badge = {
            if (pending) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.semantics { contentDescription = pendingText },
                )
            }
        },
    ) {
        FilledIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_save_device),
                contentDescription = stringResource(R.string.save_main_key),
                tint = if (failed) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        }
    }
}
