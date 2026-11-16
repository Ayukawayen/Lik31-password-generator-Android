package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ayukawayen.lik31.R

/** Height of the name tab sitting on the card's top edge; half of it overlaps the card. */
private val TabHeight = 28.dp

/** Space between the blocks of the card. */
private val BlockSpacing = 8.dp

/**
 * The generator card. Tapping outside the card closes the app; tapping the
 * password (or the keyboard's Done key) copies and closes.
 */
@Composable
fun GeneratorDialog(
    state: GeneratorState,
    onEvent: (GeneratorEvent) -> Unit,
    onCopy: () -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mainKeyFocus = remember { FocusRequester() }
    val serviceKeyFocus = remember { FocusRequester() }
    val symbolFocus = remember { FocusRequester() }

    // With a saved main key, typing starts at the service key (as in the old app).
    // Also runs when saving is switched on or off, moving on to / back to the right field.
    // While the saved key is still locked the main key field keeps the focus, ready for
    // "type it instead"; a successful unlock then moves on to the service key.
    LaunchedEffect(state.hasSavedMainKey, state.savedLocked) {
        if (state.hasSavedMainKey && !state.savedLocked) {
            serviceKeyFocus.requestFocus()
        } else {
            mainKeyFocus.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.close),
                onClick = onDismiss,
            )
            // Keeps the card clear of system bars and above the keyboard.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Card plus the name tab on its top edge. Taps here must not reach the dismiss handler.
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TabHeight / 2),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(BlockSpacing),
                ) {
                    MainKeyField(
                        value = state.mainKey,
                        mode = state.mainKeyMode,
                        visible = state.mainKeyVisible,
                        canReveal = state.canRevealMainKey,
                        locked = state.savedLocked,
                        saveEnabled = state.saveMainKey,
                        savePending = state.mainKeySavePending,
                        focusRequester = mainKeyFocus,
                        onValueChange = { onEvent(GeneratorEvent.MainKeyChanged(it)) },
                        onToggleVisibility = { onEvent(GeneratorEvent.ToggleMainKeyVisibility) },
                        onSaveToggle = { onEvent(GeneratorEvent.SaveMainKeyToggled(it)) },
                        onUnlock = onUnlock,
                        onNext = { serviceKeyFocus.requestFocus() },
                    )
                    ServiceKeyRow(
                        serviceKey = state.serviceKey,
                        symbol = state.symbol,
                        configApplied = state.configApplied,
                        edgeWhitespace = state.serviceKeyEdgeWhitespace,
                        serviceKeyFocus = serviceKeyFocus,
                        symbolFocus = symbolFocus,
                        onServiceKeyChange = { onEvent(GeneratorEvent.ServiceKeyChanged(it)) },
                        onSymbolChange = { onEvent(GeneratorEvent.SymbolChanged(it)) },
                        onDone = onCopy,
                    )
                    PasswordBox(
                        password = state.password,
                        onCopy = onCopy,
                    )
                    LengthSlider(
                        length = state.length,
                        weak = state.lengthIsWeak,
                        onLengthChange = { onEvent(GeneratorEvent.LengthChanged(it)) },
                    )
                }
            }
            NameTab(modifier = Modifier.padding(start = 12.dp))
        }

        if (state.confirmDisableSave) {
            AlertDialog(
                onDismissRequest = { onEvent(GeneratorEvent.DisableSaveCancelled) },
                title = { Text(stringResource(R.string.disable_save_title)) },
                text = { Text(stringResource(R.string.disable_save_text)) },
                confirmButton = {
                    TextButton(onClick = { onEvent(GeneratorEvent.DisableSaveConfirmed) }) {
                        Text(stringResource(R.string.disable_save_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(GeneratorEvent.DisableSaveCancelled) }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

/** "⚿ Lik31" pill on the card's top edge, in place of a title row. */
@Composable
private fun NameTab(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(TabHeight),
        shape = RoundedCornerShape(TabHeight / 2),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tile),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
