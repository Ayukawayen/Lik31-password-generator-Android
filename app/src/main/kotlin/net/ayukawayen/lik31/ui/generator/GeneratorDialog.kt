package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.ayukawayen.lik31.R

/**
 * The generator card. Tapping outside the card closes the app; tapping the
 * password (or the keyboard's Done key) copies and closes.
 */
@Composable
fun GeneratorDialog(
    state: GeneratorState,
    onEvent: (GeneratorEvent) -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mainKeyFocus = remember { FocusRequester() }
    val serviceKeyFocus = remember { FocusRequester() }
    val symbolFocus = remember { FocusRequester() }

    // Nothing is saved in the MVP, so typing always starts at the main key.
    LaunchedEffect(Unit) { mainKeyFocus.requestFocus() }

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
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                // Swallow taps on the card so they do not reach the dismiss handler.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.generator_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                MainKeyField(
                    value = state.mainKey,
                    visible = state.mainKeyVisible,
                    focusRequester = mainKeyFocus,
                    onValueChange = { onEvent(GeneratorEvent.MainKeyChanged(it)) },
                    onToggleVisibility = { onEvent(GeneratorEvent.ToggleMainKeyVisibility) },
                    onNext = { serviceKeyFocus.requestFocus() },
                )
                ServiceKeyRow(
                    serviceKey = state.serviceKey,
                    symbol = state.symbol,
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
                    onLengthChange = { onEvent(GeneratorEvent.LengthChanged(it)) },
                )
            }
        }
    }
}
