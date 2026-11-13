package net.ayukawayen.lik31.ui.generator

import net.ayukawayen.lik31.core.Lik31
import net.ayukawayen.lik31.data.ServiceConfig

/**
 * Everything the generator screen shows. Pure Kotlin (no Android imports) so
 * the whole input -> password behaviour is unit-testable on the JVM.
 *
 * [mainHash] and [raw] are caches derived from the inputs; always change state
 * through [reduce] so they stay consistent.
 */
data class GeneratorState(
    val mainKey: String = "",
    val serviceKey: String = "",
    val symbol: String = "",
    val length: Int = Lik31.DEFAULT_LENGTH,
    val mainKeyVisible: Boolean = false,

    /** Position of the "save main key" switch. */
    val saveMainKey: Boolean = false,
    /** The main key hash stored on this device, if any. */
    val savedHash: String? = null,
    /** True while the "stop saving?" confirmation is showing. */
    val confirmDisableSave: Boolean = false,
    /** True after storing the main key failed. */
    val saveFailed: Boolean = false,

    /** True while length/symbol are the ones remembered for this service. */
    val configApplied: Boolean = false,
    /** Last length / symbol the user chose by hand; used for services with nothing remembered. */
    val fallbackLength: Int = Lik31.DEFAULT_LENGTH,
    val fallbackSymbol: String = "",

    /** Hash in effect: of the typed main key, or the saved one while the field is empty. */
    val mainHash: String = EMPTY_MAIN_HASH,
    /** 75-character raw password, or "" while the service key is empty. */
    val raw: String = "",
) {
    /** What is displayed and copied; "" means "nothing to copy yet". */
    val password: String
        get() = if (raw.isEmpty()) "" else Lik31.wrap(raw, symbol, length)

    val canCopy: Boolean
        get() = raw.isNotEmpty()

    val hasSavedMainKey: Boolean
        get() = savedHash != null

    /** True when the typed main key should be written to the vault. An empty main key is never saved. */
    val shouldPersistMainKey: Boolean
        get() = saveMainKey && mainKey.isNotEmpty() && mainHash != savedHash

    val normalizedServiceKey: String
        get() = Lik31.normalizeServiceKey(serviceKey)

    /** How the main key field presents itself; see [MainKeyMode]. */
    val mainKeyMode: MainKeyMode
        get() = when {
            saveFailed -> MainKeyMode.SaveFailed
            savedHash == null -> MainKeyMode.Normal
            mainKey.isEmpty() -> MainKeyMode.UsingSaved
            mainHash == savedHash -> MainKeyMode.RetypedSaved
            else -> MainKeyMode.ReplacingSaved
        }

    /**
     * The eye button is offered only when there is typed text to reveal.
     * A saved main key exists only as a hash: its characters and even its
     * length are unknown, so there is nothing to show.
     */
    val canRevealMainKey: Boolean
        get() = mainKeyMode != MainKeyMode.UsingSaved

    /** The typed main key will be written to the vault when a password is copied. */
    val mainKeySavePending: Boolean
        get() = shouldPersistMainKey

    /** What to remember for the current service when the password is copied. */
    val configToRemember: ServiceConfig?
        get() = if (canCopy) ServiceConfig(length, symbol) else null

    /** Never print secrets (main key, its hash, or passwords) into logs. */
    override fun toString(): String =
        "GeneratorState(serviceKeyLength=${serviceKey.length}, length=$length, hasSymbol=${symbol.isNotEmpty()}, " +
            "saveMainKey=$saveMainKey, hasSavedMainKey=$hasSavedMainKey)"

    companion object {
        /** Hash of the empty main key; the browser extension starts from the same value. */
        val EMPTY_MAIN_HASH: String = Lik31.hashMainKey("")
    }
}

/** Presentation of the main key field. */
enum class MainKeyMode {
    /** Nothing saved: an ordinary masked input. */
    Normal,

    /** A key is saved and the field is empty: the saved hash is in use. Nothing to display or reveal. */
    UsingSaved,

    /** A key is saved and the user typed that same key again. */
    RetypedSaved,

    /** A key is saved and a different one is typed: it is used now and replaces the saved one on copy. */
    ReplacingSaved,

    /** Storing the main key failed; cleared by the next edit or attempt. */
    SaveFailed,
}

sealed interface GeneratorEvent {
    data class MainKeyChanged(val value: String) : GeneratorEvent

    /**
     * @param saved what is remembered for this service key, or null. The UI
     * sends the event without it; the view model looks it up and fills it in.
     */
    data class ServiceKeyChanged(val value: String, val saved: ServiceConfig? = null) : GeneratorEvent
    data class SymbolChanged(val value: String) : GeneratorEvent
    data class LengthChanged(val value: Int) : GeneratorEvent
    data object ToggleMainKeyVisibility : GeneratorEvent

    /** Start-up: what the vault holds (null = nothing saved). */
    data class SavedHashLoaded(val hash: String?) : GeneratorEvent
    data class SaveMainKeyToggled(val on: Boolean) : GeneratorEvent
    data object DisableSaveConfirmed : GeneratorEvent
    data object DisableSaveCancelled : GeneratorEvent

    /** The hash currently in effect was written to the vault. */
    data object MainKeyPersisted : GeneratorEvent
    data object MainKeyPersistFailed : GeneratorEvent
}

fun GeneratorState.reduce(event: GeneratorEvent): GeneratorState = when (event) {
    is GeneratorEvent.MainKeyChanged -> copy(mainKey = event.value, saveFailed = false).recomputed()

    is GeneratorEvent.ServiceKeyChanged -> {
        // Same rule as the browser extension: a remembered service restores its
        // settings; any other service gets the last hand-picked ones.
        val saved = event.saved
        copy(
            serviceKey = event.value,
            length = if (saved != null) Lik31.clampLength(saved.length) else fallbackLength,
            symbol = if (saved != null) sanitizeSymbol(saved.symbol, "") else fallbackSymbol,
            configApplied = saved != null,
        ).recomputed()
    }

    is GeneratorEvent.SymbolChanged -> {
        val symbol = sanitizeSymbol(event.value, symbol)
        copy(symbol = symbol, fallbackSymbol = symbol, configApplied = false)
    }

    is GeneratorEvent.LengthChanged -> {
        val length = Lik31.clampLength(event.value)
        copy(length = length, fallbackLength = length, configApplied = false)
    }

    GeneratorEvent.ToggleMainKeyVisibility -> copy(mainKeyVisible = !mainKeyVisible)

    is GeneratorEvent.SavedHashLoaded ->
        copy(savedHash = event.hash, saveMainKey = event.hash != null).recomputed()

    is GeneratorEvent.SaveMainKeyToggled -> when {
        event.on -> copy(saveMainKey = true, saveFailed = false)
        // Turning it off deletes a stored key, so ask first.
        savedHash != null -> copy(confirmDisableSave = true)
        else -> copy(saveMainKey = false, saveFailed = false)
    }

    GeneratorEvent.DisableSaveConfirmed ->
        copy(savedHash = null, saveMainKey = false, confirmDisableSave = false, saveFailed = false).recomputed()

    GeneratorEvent.DisableSaveCancelled -> copy(confirmDisableSave = false)

    GeneratorEvent.MainKeyPersisted -> copy(savedHash = mainHash, saveFailed = false)

    // Keep the switch on only if an older key is still stored.
    GeneratorEvent.MainKeyPersistFailed -> copy(saveMainKey = savedHash != null, saveFailed = true)
}

/**
 * Re-derives [GeneratorState.mainHash] and [GeneratorState.raw].
 *
 * While the main key field is empty and a hash is saved, the saved hash is
 * used. (The browser extension instead overwrites the saved key with the
 * empty key when its field is cleared; the app deliberately does not.)
 */
private fun GeneratorState.recomputed(): GeneratorState {
    val hash = if (mainKey.isEmpty() && savedHash != null) savedHash else Lik31.hashMainKey(mainKey)
    return copy(mainHash = hash, raw = computeRaw(hash, serviceKey))
}

/**
 * The app shows no password while the service key is empty. (The browser
 * extension does produce one for an empty service key; that is a UI choice
 * and does not affect any password for a non-empty service key.)
 */
private fun computeRaw(mainHash: String, serviceKey: String): String =
    if (serviceKey.isEmpty()) "" else Lik31.rawPassword(mainHash, Lik31.normalizeServiceKey(serviceKey))

/**
 * Reduces whatever is in the symbol field to at most one character.
 *
 * The browser extension uses only the first UTF-16 code unit of the symbol, so
 * a character outside the BMP (an emoji) would put half a surrogate pair into
 * the password. Such characters and control characters are rejected. Typing a
 * second character replaces the first.
 */
internal fun sanitizeSymbol(input: String, current: String): String {
    if (input.isEmpty()) return ""
    val picked = input.lastOrNull { !it.isSurrogate() && !it.isISOControl() }
    return picked?.toString() ?: current
}
