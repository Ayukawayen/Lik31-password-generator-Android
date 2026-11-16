package net.ayukawayen.lik31.entry

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import net.ayukawayen.lik31.Lik31App
import net.ayukawayen.lik31.system.ClipboardWriter
import net.ayukawayen.lik31.ui.generator.GeneratorDialog
import net.ayukawayen.lik31.ui.generator.GeneratorViewModel
import net.ayukawayen.lik31.ui.theme.Lik31Theme
import net.ayukawayen.lik31.ui.unlock.BiometricUnlock

/**
 * The generator: a dialog-style card over a dimmed background.
 * Type the keys, tap the password, and the app copies it and closes.
 */
class GeneratorActivity : ComponentActivity() {
    private val viewModel: GeneratorViewModel by viewModels()

    /** True while the biometric prompt is on screen. */
    private var unlocking = false
    private var stopped = false

    /** True while the settings screen is on top; the generator then survives onStop. */
    private var settingsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecureWindowSetting()
        enableEdgeToEdge()

        setContent {
            Lik31Theme {
                GeneratorDialog(
                    state = viewModel.state,
                    onEvent = viewModel::onEvent,
                    onCopy = ::copyAndClose,
                    onUnlock = ::requestUnlock,
                    onOpenSettings = ::openSettings,
                    onDismiss = ::finish,
                )
            }
        }

        // A saved, biometric-protected main key: ask for the biometric right away.
        if (!viewModel.autoUnlockOffered) {
            viewModel.autoUnlockOffered = true
            requestUnlock()
        }
    }

    override fun onStart() {
        super.onStart()
        stopped = false
        settingsOpen = false
        // May have been changed on the settings screen.
        applySecureWindowSetting()
    }

    override fun onStop() {
        super.onStop()
        stopped = true
        // Leaving the screen (home, app switch, screen off) discards everything
        // typed, so a typed main key never lingers in memory. Rotation is exempt,
        // and so are the biometric prompt (in case it stops this activity) and the
        // settings screen (which closes the whole app itself if the user leaves from there).
        if (!isChangingConfigurations && !unlocking && !settingsOpen) finish()
    }

    /** Keeps keys and passwords out of screenshots, screen recordings and the recents preview. */
    private fun applySecureWindowSetting() {
        val secure = (application as Lik31App).settings.secureWindow
        window.setFlags(
            if (secure) WindowManager.LayoutParams.FLAG_SECURE else 0,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }

    private fun openSettings() {
        settingsOpen = true
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun requestUnlock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || unlocking) return
        val cipher = viewModel.beginUnlock() ?: return
        unlocking = true
        BiometricUnlock.show(
            activity = this,
            cipher = cipher,
            onSuccess = {
                viewModel.completeUnlock(it)
                unlockEnded()
            },
            onDismissed = ::unlockEnded,
        )
    }

    private fun unlockEnded() {
        unlocking = false
        // The prompt went away because the user left the app: close as onStop would have.
        if (stopped && !isFinishing) finish()
    }

    private fun copyAndClose() {
        val password = viewModel.state.password
        if (password.isEmpty()) return
        // Remember this service's settings (and the main key, if saving is on).
        viewModel.onCopied()
        ClipboardWriter.copySensitive(this, password)
        finish()
    }
}
