package net.ayukawayen.lik31.ui.generator

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import net.ayukawayen.lik31.Lik31App
import net.ayukawayen.lik31.core.Lik31

/**
 * Holds the screen state across configuration changes and connects the pure
 * state logic ([GeneratorState.reduce]) to storage. Hashing and lookups are
 * fast enough to run synchronously on each keystroke.
 */
class GeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val lik31App = application as Lik31App
    private val vault = lik31App.mainKeyVault
    private val serviceConfigs = lik31App.serviceConfigs

    var state by mutableStateOf(GeneratorState().reduce(GeneratorEvent.SavedHashLoaded(vault.load())))
        private set

    fun onEvent(event: GeneratorEvent) {
        val resolved = if (event is GeneratorEvent.ServiceKeyChanged) {
            event.copy(saved = serviceConfigs.load(Lik31.normalizeServiceKey(event.value)))
        } else {
            event
        }
        state = state.reduce(resolved)

        when (resolved) {
            // Switch turned on while a main key is already typed: save it now.
            is GeneratorEvent.SaveMainKeyToggled -> persistMainKeyIfNeeded()
            GeneratorEvent.DisableSaveConfirmed -> vault.clear()
            else -> Unit
        }
    }

    /** Call when the password is being copied: the moment settings are committed. */
    fun onCopied() {
        persistMainKeyIfNeeded()
        state.configToRemember?.let { serviceConfigs.save(state.normalizedServiceKey, it) }
    }

    private fun persistMainKeyIfNeeded() {
        if (!state.shouldPersistMainKey) return
        state = try {
            vault.save(state.mainHash)
            state.reduce(GeneratorEvent.MainKeyPersisted)
        } catch (e: Exception) {
            state.reduce(GeneratorEvent.MainKeyPersistFailed)
        }
    }
}
