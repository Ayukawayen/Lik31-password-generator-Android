package net.ayukawayen.lik31.entry

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.ayukawayen.lik31.Lik31App
import net.ayukawayen.lik31.R
import net.ayukawayen.lik31.core.SelfCheck
import net.ayukawayen.lik31.ui.settings.SettingsScreen
import net.ayukawayen.lik31.ui.settings.SettingsUiState
import net.ayukawayen.lik31.ui.theme.Lik31Theme

/** Full-screen settings, opened from the round button on the generator card. */
class SettingsActivity : ComponentActivity() {
    private val lik31App: Lik31App get() = application as Lik31App

    private var uiState by mutableStateOf(SettingsUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        uiState = readState()

        setContent {
            Lik31Theme {
                SettingsScreen(
                    state = uiState,
                    onBack = ::finish,
                    onDefaultLengthChange = {
                        lik31App.settings.defaultLength = it
                        uiState = uiState.copy(defaultLength = lik31App.settings.defaultLength)
                    },
                    onProtectWithBiometricsChange = {
                        lik31App.settings.protectWithBiometrics = it
                        uiState = uiState.copy(protectWithBiometrics = it)
                    },
                    onSecureWindowChange = {
                        lik31App.settings.secureWindow = it
                        uiState = uiState.copy(secureWindow = it)
                    },
                    onClearServiceConfigs = {
                        lik31App.serviceConfigs.clearAll()
                        uiState = uiState.copy(serviceConfigCount = 0)
                    },
                    onAddTile = ::requestAddTile,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app from here (home, app switch, screen off) also closes the
        // generator waiting underneath, so nothing typed there lingers. Going back
        // (isFinishing) or rotating keeps it.
        if (!isFinishing && !isChangingConfigurations) finishAffinity()
    }

    private fun readState(): SettingsUiState {
        val settings = lik31App.settings
        return SettingsUiState(
            defaultLength = settings.defaultLength,
            protectWithBiometrics = settings.protectWithBiometrics,
            biometricsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            secureWindow = settings.secureWindow,
            serviceConfigCount = lik31App.serviceConfigs.count(),
            selfCheckPassed = SelfCheck.passed(),
            selfCheckTotal = SelfCheck.caseCount,
            versionName = try {
                packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
            } catch (e: Exception) {
                ""
            },
            canAddTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        )
    }

    /** Android 13+: asks the system to offer adding the Quick Settings tile. */
    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val statusBar = getSystemService(StatusBarManager::class.java) ?: return
        statusBar.requestAddTileService(
            ComponentName(this, GeneratorTileService::class.java),
            getString(R.string.tile_label),
            Icon.createWithResource(this, R.drawable.ic_tile),
            mainExecutor,
        ) { }
    }
}
