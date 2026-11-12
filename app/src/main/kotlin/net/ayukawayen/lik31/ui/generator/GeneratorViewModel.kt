package net.ayukawayen.lik31.ui.generator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Holds the screen state across configuration changes (rotation). All logic
 * lives in [GeneratorState.reduce]; hashing is fast enough (two SHA-256 calls)
 * to run synchronously on each keystroke.
 */
class GeneratorViewModel : ViewModel() {
    var state by mutableStateOf(GeneratorState())
        private set

    fun onEvent(event: GeneratorEvent) {
        state = state.reduce(event)
    }
}
