package net.ayukawayen.lik31.ui.generator

import net.ayukawayen.lik31.core.Lik31

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
    val mainHash: String = EMPTY_MAIN_HASH,
    /** 75-character raw password, or "" while the service key is empty. */
    val raw: String = "",
) {
    /** What is displayed and copied; "" means "nothing to copy yet". */
    val password: String
        get() = if (raw.isEmpty()) "" else Lik31.wrap(raw, symbol, length)

    val canCopy: Boolean
        get() = raw.isNotEmpty()

    /** Never print secrets (main key, its hash, or passwords) into logs. */
    override fun toString(): String =
        "GeneratorState(serviceKeyLength=${serviceKey.length}, length=$length, hasSymbol=${symbol.isNotEmpty()})"

    companion object {
        /** Hash of the empty main key; the browser extension starts from the same value. */
        val EMPTY_MAIN_HASH: String = Lik31.hashMainKey("")
    }
}

sealed interface GeneratorEvent {
    data class MainKeyChanged(val value: String) : GeneratorEvent
    data class ServiceKeyChanged(val value: String) : GeneratorEvent
    data class SymbolChanged(val value: String) : GeneratorEvent
    data class LengthChanged(val value: Int) : GeneratorEvent
    data object ToggleMainKeyVisibility : GeneratorEvent
}

fun GeneratorState.reduce(event: GeneratorEvent): GeneratorState = when (event) {
    is GeneratorEvent.MainKeyChanged -> {
        val hash = Lik31.hashMainKey(event.value)
        copy(mainKey = event.value, mainHash = hash, raw = computeRaw(hash, serviceKey))
    }
    is GeneratorEvent.ServiceKeyChanged ->
        copy(serviceKey = event.value, raw = computeRaw(mainHash, event.value))
    is GeneratorEvent.SymbolChanged -> copy(symbol = sanitizeSymbol(event.value, symbol))
    is GeneratorEvent.LengthChanged -> copy(length = Lik31.clampLength(event.value))
    GeneratorEvent.ToggleMainKeyVisibility -> copy(mainKeyVisible = !mainKeyVisible)
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
