package net.ayukawayen.lik31.entry

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import net.ayukawayen.lik31.system.ClipboardWriter
import net.ayukawayen.lik31.ui.generator.GeneratorDialog
import net.ayukawayen.lik31.ui.generator.GeneratorViewModel
import net.ayukawayen.lik31.ui.theme.Lik31Theme

/**
 * The generator: a dialog-style card over a dimmed background.
 * Type the keys, tap the password, and the app copies it and closes.
 */
class GeneratorActivity : ComponentActivity() {
    private val viewModel: GeneratorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep keys and passwords out of screenshots, screen recordings and the recents preview.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        setContent {
            Lik31Theme {
                GeneratorDialog(
                    state = viewModel.state,
                    onEvent = viewModel::onEvent,
                    onCopy = ::copyAndClose,
                    onDismiss = ::finish,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the screen (home, app switch, screen off) discards everything
        // typed, so a typed main key never lingers in memory. Rotation is exempt.
        if (!isChangingConfigurations) finish()
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
