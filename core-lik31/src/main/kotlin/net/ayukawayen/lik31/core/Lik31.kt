package net.ayukawayen.lik31.core

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * Lik31 deterministic password algorithm, bit-for-bit compatible with the
 * "Lik31 Password Generator" browser extension (popup.js, version 3.0.0).
 *
 *     mh  = hex(SHA-256(bits(UTF-8(mainKey))))     bits = each byte as 8 chars '0'/'1'
 *     sn  = lowercase(NFC(serviceKey))
 *     raw = groups(SHA-256(UTF-8(mh + sn)))        75 characters
 *     pw  = (raw with symbol inserted at index 5).take(length)
 *
 * Every function here is pure and safe to call from any thread.
 */
object Lik31 {
    const val MIN_LENGTH: Int = 6
    const val MAX_LENGTH: Int = 75
    const val DEFAULT_LENGTH: Int = 20

    /** Length of [rawPassword]'s result. */
    const val RAW_LENGTH: Int = 75

    /** Index at which the symbol is inserted (after the first group). */
    const val SYMBOL_INDEX: Int = 5

    private val HEX = "0123456789abcdef".toCharArray()

    /**
     * Hash of the main key: 64 lower-case hex characters.
     * The main key is used exactly as typed (no trimming, no normalization).
     * The result is as sensitive as the main key itself.
     */
    fun hashMainKey(mainKey: String): String {
        val utf8 = Utf8.encode(mainKey)
        val bits = ByteArray(utf8.size * 8)
        for (i in utf8.indices) {
            val b = utf8[i].toInt() and 0xFF
            for (bit in 0 until 8) {
                bits[i * 8 + bit] = if ((b shr (7 - bit)) and 1 == 1) '1'.code.toByte() else '0'.code.toByte()
            }
        }
        return toHex(sha256(bits))
    }

    /**
     * Canonical form of the service key: NFC, then locale-independent lower case.
     * Whitespace is preserved (leading/trailing spaces change the password).
     */
    fun normalizeServiceKey(serviceKey: String): String =
        // Locale.ROOT is essential: with a Turkish default locale "I" would
        // become "ı" and no longer match the browser's String.toLowerCase().
        Normalizer.normalize(serviceKey, Normalizer.Form.NFC).lowercase(Locale.ROOT)

    /**
     * The full 75-character password before symbol insertion and truncation.
     * @param mainHash result of [hashMainKey]
     * @param normalizedServiceKey result of [normalizeServiceKey]
     */
    fun rawPassword(mainHash: String, normalizedServiceKey: String): String =
        ChunkEncoder.encode(sha256(Utf8.encode(mainHash + normalizedServiceKey)))

    /**
     * Inserts the symbol and truncates. Mirrors the browser exactly:
     * only the first UTF-16 code unit of [symbol] is used, and [length] is
     * applied as given (use [clampLength] for user input).
     */
    fun wrap(raw: String, symbol: String, length: Int): String {
        val withSymbol = if (symbol.isNotEmpty()) {
            raw.take(SYMBOL_INDEX) + symbol[0] + raw.drop(SYMBOL_INDEX)
        } else {
            raw
        }
        return withSymbol.take(length.coerceAtLeast(0))
    }

    /** Clamps a user-chosen length to the range the browser extension allows. */
    fun clampLength(length: Int): Int = length.coerceIn(MIN_LENGTH, MAX_LENGTH)

    /** Convenience: the whole pipeline in one call. */
    fun generate(mainKey: String, serviceKey: String, symbol: String, length: Int): String =
        wrap(rawPassword(hashMainKey(mainKey), normalizeServiceKey(serviceKey)), symbol, length)

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX[b ushr 4]
            out[i * 2 + 1] = HEX[b and 0x0F]
        }
        return String(out)
    }
}
