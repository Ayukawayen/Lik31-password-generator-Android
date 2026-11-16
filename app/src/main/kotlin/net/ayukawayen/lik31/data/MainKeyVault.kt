package net.ayukawayen.lik31.data

import javax.crypto.Cipher

/** What the vault holds, as far as it can tell without asking the user. */
sealed interface StoredMainKey {
    data object None : StoredMainKey

    /** Stored without biometric protection: readable right away. */
    data class Unlocked(val mainHash: String) : StoredMainKey {
        /** Never print the hash. */
        override fun toString(): String = "Unlocked"
    }

    /** Stored with biometric protection: see [MainKeyVault.beginUnlock]. */
    data object Locked : StoredMainKey
}

/**
 * Stores the main key HASH (never the main key itself) on the device.
 * The hash is as sensitive as the main key: whoever has it can generate
 * every password.
 *
 * Saving never needs the user (so "copy and close" stays one action); reading
 * a biometric-protected hash does:
 *
 *     read() == Locked  ->  beginUnlock()  ->  biometric prompt with that cipher
 *                       ->  completeUnlock(cipher)
 */
interface MainKeyVault {
    /** Never prompts. */
    fun read(): StoredMainKey

    /**
     * Stores the hash, biometric-protected when the device supports it.
     * @throws Exception when the hash could not be stored
     */
    fun save(mainHash: String)

    /** Deletes the saved hash and its keys. */
    fun clear()

    /**
     * Prepares the cipher a biometric prompt must authorise. Returns null when
     * the stored key can no longer be unlocked (for example fingerprints were
     * added or removed); the vault has then been cleared.
     */
    fun beginUnlock(): Cipher?

    /** @return the hash, or null if decryption failed (nothing is cleared) */
    fun completeUnlock(authorisedCipher: Cipher): String?

    companion object {
        /** A main key hash is exactly 64 lower-case hex characters. */
        fun isValidHash(text: String): Boolean =
            text.length == 64 && text.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
