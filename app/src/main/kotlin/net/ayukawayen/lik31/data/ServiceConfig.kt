package net.ayukawayen.lik31.data

import net.ayukawayen.lik31.core.Lik31
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * What the app remembers per service. Neither value is secret, and neither
 * changes the algorithm: they only save the user from re-entering them.
 * (The browser extension remembers the length only.)
 */
data class ServiceConfig(val length: Int, val symbol: String)

/** Remembers [ServiceConfig]s, keyed by the NORMALIZED service key. */
interface ServiceConfigStore {
    fun load(normalizedServiceKey: String): ServiceConfig?
    fun save(normalizedServiceKey: String, config: ServiceConfig)
}

/** Text form of a [ServiceConfig]: "<length>:<symbol>", e.g. "14:@" or "20:". */
object ServiceConfigCodec {
    fun encode(config: ServiceConfig): String = "${config.length}:${config.symbol}"

    /** Returns null for anything malformed; out-of-range values are repaired. */
    fun decode(text: String): ServiceConfig? {
        val sep = text.indexOf(':')
        if (sep <= 0) return null
        val length = text.substring(0, sep).toIntOrNull() ?: return null
        val symbol = text.substring(sep + 1)
            .firstOrNull { !it.isSurrogate() && !it.isISOControl() }
            ?.toString()
            .orEmpty()
        return ServiceConfig(Lik31.clampLength(length), symbol)
    }
}

/**
 * Turns a service key into the storage key "sc:<hex HMAC-SHA256>", so the list
 * of services is not stored in readable form. The salt is random per install
 * and stored next to the data: this hides service names from a casual look at
 * the file, it is not protection against someone who has the file AND guesses
 * names (the same trade-off as the browser extension's hashed storage keys).
 */
class ServiceKeyIndex(salt: ByteArray) {
    private val keySpec = SecretKeySpec(salt.copyOf(), ALGORITHM)

    fun storageKey(normalizedServiceKey: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(keySpec)
        return PREFIX + Hex.encode(mac.doFinal(normalizedServiceKey.toByteArray(Charsets.UTF_8)))
    }

    companion object {
        const val PREFIX = "sc:"
        const val SALT_BYTES = 32
        private const val ALGORITHM = "HmacSHA256"
    }
}

object Hex {
    private val DIGITS = "0123456789abcdef".toCharArray()

    fun encode(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            out[i * 2] = DIGITS[b ushr 4]
            out[i * 2 + 1] = DIGITS[b and 0x0F]
        }
        return String(out)
    }

    /** Returns null unless [text] is an even number of hex digits. */
    fun decode(text: String): ByteArray? {
        if (text.length % 2 != 0) return null
        val out = ByteArray(text.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(text[i * 2], 16)
            val lo = Character.digit(text[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
