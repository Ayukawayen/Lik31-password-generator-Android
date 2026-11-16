package net.ayukawayen.lik31.ui.generator

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import net.ayukawayen.lik31.Lik31App
import net.ayukawayen.lik31.core.Lik31
import net.ayukawayen.lik31.data.StoredMainKey
import javax.crypto.Cipher

/**
 * Holds the screen state across configuration changes and connects the pure
 * state logic ([GeneratorState.reduce]) to storage. Hashing and lookups are
 * fast enough to run synchronously on each keystroke.
 */
class GeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val lik31App = application as Lik31App
    private val vault = lik31App.mainKeyVault
    private val serviceConfigs = lik31App.serviceConfigs

    var state by mutableStateOf(GeneratorState().reduce(initialVaultEvent()))
        private set

    /** The automatic unlock prompt is offered once per launch, not again after a rotation. */
    var autoUnlockOffered: Boolean = false

    private fun initialVaultEvent(): GeneratorEvent = when (val stored = vault.read()) {
        StoredMainKey.None -> GeneratorEvent.SavedHashLoaded(null)
        is StoredMainKey.Unlocked -> GeneratorEvent.SavedHashLoaded(stored.mainHash)
        StoredMainKey.Locked -> GeneratorEvent.SavedKeyLocked
    }

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

    /**
     * First half of a biometric unlock: the cipher the prompt must authorise.
     * Null when there is nothing to unlock, or the stored key turned out to be
     * unusable (the state then asks for the main key to be typed again).
     */
    fun beginUnlock(): Cipher? {
        if (!state.savedLocked) return null
        val cipher = vault.beginUnlock()
        if (cipher == null) state = state.reduce(GeneratorEvent.SavedKeyInvalidated)
        return cipher
    }

    /** Second half: the prompt succeeded. On a decryption failure the key simply stays locked. */
    fun completeUnlock(authorisedCipher: Cipher) {
        val hash = vault.completeUnlock(authorisedCipher) ?: return
        state = state.reduce(GeneratorEvent.SavedHashLoaded(hash))
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
