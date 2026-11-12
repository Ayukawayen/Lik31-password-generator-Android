package net.ayukawayen.lik31.data

/**
 * Stores the main key HASH (never the main key itself) on the device.
 * The hash is as sensitive as the main key: whoever has it can generate
 * every password.
 */
interface MainKeyVault {
    /** The saved hash, or null when nothing is saved or it can no longer be read. */
    fun load(): String?

    /** @throws Exception when the hash could not be stored */
    fun save(mainHash: String)

    /** Deletes the saved hash and its encryption key. */
    fun clear()

    companion object {
        /** A main key hash is exactly 64 lower-case hex characters. */
        fun isValidHash(text: String): Boolean =
            text.length == 64 && text.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
