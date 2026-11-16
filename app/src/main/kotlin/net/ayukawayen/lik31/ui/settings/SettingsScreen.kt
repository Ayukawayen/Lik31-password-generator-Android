package net.ayukawayen.lik31.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ayukawayen.lik31.R
import net.ayukawayen.lik31.core.Lik31

/** Everything the settings screen shows. */
data class SettingsUiState(
    val defaultLength: Int = Lik31.DEFAULT_LENGTH,
    val protectWithBiometrics: Boolean = true,
    /** False on Android versions where the app cannot offer biometric protection. */
    val biometricsSupported: Boolean = true,
    val secureWindow: Boolean = true,
    val serviceConfigCount: Int = 0,
    val selfCheckPassed: Int = 0,
    val selfCheckTotal: Int = 0,
    val versionName: String = "",
    val canAddTile: Boolean = false,
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onDefaultLengthChange: (Int) -> Unit,
    onProtectWithBiometricsChange: (Boolean) -> Unit,
    onSecureWindowChange: (Boolean) -> Unit,
    onClearServiceConfigs: () -> Unit,
    onAddTile: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            SectionTitle(R.string.settings_section_generate)
            SettingRow(
                title = stringResource(R.string.settings_default_length),
                subtitle = stringResource(R.string.settings_default_length_hint),
            ) {
                LengthStepper(value = state.defaultLength, onChange = onDefaultLengthChange)
            }

            SectionTitle(R.string.settings_section_security)
            SettingRow(
                title = stringResource(R.string.settings_biometrics),
                subtitle = stringResource(
                    if (state.biometricsSupported) {
                        R.string.settings_biometrics_hint
                    } else {
                        R.string.settings_biometrics_unsupported
                    },
                ),
            ) {
                Switch(
                    checked = state.protectWithBiometrics && state.biometricsSupported,
                    onCheckedChange = onProtectWithBiometricsChange,
                    enabled = state.biometricsSupported,
                )
            }
            SettingRow(
                title = stringResource(R.string.settings_secure_window),
                subtitle = stringResource(R.string.settings_secure_window_hint),
            ) {
                Switch(checked = state.secureWindow, onCheckedChange = onSecureWindowChange)
            }

            SectionTitle(R.string.settings_section_services)
            SettingRow(
                title = stringResource(R.string.settings_service_count, state.serviceConfigCount),
                subtitle = stringResource(R.string.settings_service_count_hint),
            ) {
                TextButton(
                    onClick = { confirmClear = true },
                    enabled = state.serviceConfigCount > 0,
                ) {
                    Text(stringResource(R.string.settings_clear_all))
                }
            }

            if (state.canAddTile) {
                SectionTitle(R.string.settings_section_shortcut)
                SettingRow(
                    title = stringResource(R.string.settings_add_tile),
                    subtitle = stringResource(R.string.settings_add_tile_hint),
                ) {
                    TextButton(onClick = onAddTile) {
                        Text(stringResource(R.string.settings_add))
                    }
                }
            }

            SectionTitle(R.string.settings_section_about)
            SettingRow(
                title = stringResource(R.string.settings_algorithm),
                subtitle = stringResource(R.string.settings_algorithm_hint),
            )
            val checkOk = state.selfCheckTotal > 0 && state.selfCheckPassed == state.selfCheckTotal
            SettingRow(
                title = stringResource(R.string.settings_self_check),
                subtitle = stringResource(
                    if (checkOk) R.string.settings_self_check_ok else R.string.settings_self_check_failed,
                    state.selfCheckPassed,
                    state.selfCheckTotal,
                ),
                subtitleIsError = !checkOk,
            )
            SettingRow(
                title = stringResource(R.string.settings_version),
                subtitle = state.versionName,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_clear_title)) },
            text = { Text(stringResource(R.string.settings_clear_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearServiceConfigs()
                    },
                ) {
                    Text(stringResource(R.string.settings_clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    subtitleIsError: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (subtitleIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (trailing != null) {
            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trailing()
            }
        }
    }
}

/** − number + for the default length. */
@Composable
private fun LengthStepper(value: Int, onChange: (Int) -> Unit) {
    val decreaseLabel = stringResource(R.string.decrease_length)
    val increaseLabel = stringResource(R.string.increase_length)
    IconButton(
        onClick = { onChange(value - 1) },
        enabled = value > Lik31.MIN_LENGTH,
        modifier = Modifier.semantics { contentDescription = decreaseLabel },
    ) {
        Text("−", style = MaterialTheme.typography.titleLarge)
    }
    Text(
        text = value.toString(),
        modifier = Modifier.widthIn(min = 28.dp),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    IconButton(
        onClick = { onChange(value + 1) },
        enabled = value < Lik31.MAX_LENGTH,
        modifier = Modifier.semantics { contentDescription = increaseLabel },
    ) {
        Text("+", style = MaterialTheme.typography.titleLarge)
    }
}
