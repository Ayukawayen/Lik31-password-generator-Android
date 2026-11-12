package net.ayukawayen.lik31.core

import java.io.ByteArrayOutputStream

/**
 * UTF-8 encoding with the semantics of the browser's `TextEncoder`:
 * an unpaired surrogate is encoded as U+FFFD (EF BF BD).
 *
 * `String.toByteArray(Charsets.UTF_8)` must NOT be used for hashing input,
 * because it encodes an unpaired surrogate as '?' (0x3F) and would produce
 * different passwords from the browser extension.
 */
internal object Utf8 {
    fun encode(s: String): ByteArray {
        val out = ByteArrayOutputStream(s.length + 16)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            var cp: Int
            if (Character.isHighSurrogate(c) && i + 1 < s.length && Character.isLowSurrogate(s[i + 1])) {
                cp = Character.toCodePoint(c, s[i + 1])
                i += 2
            } else {
                cp = if (Character.isSurrogate(c)) 0xFFFD else c.code
                i += 1
            }
            when {
                cp < 0x80 -> out.write(cp)
                cp < 0x800 -> {
                    out.write(0xC0 or (cp shr 6))
                    out.write(0x80 or (cp and 0x3F))
                }
                cp < 0x10000 -> {
                    out.write(0xE0 or (cp shr 12))
                    out.write(0x80 or ((cp shr 6) and 0x3F))
                    out.write(0x80 or (cp and 0x3F))
                }
                else -> {
                    out.write(0xF0 or (cp shr 18))
                    out.write(0x80 or ((cp shr 12) and 0x3F))
                    out.write(0x80 or ((cp shr 6) and 0x3F))
                    out.write(0x80 or (cp and 0x3F))
                }
            }
        }
        return out.toByteArray()
    }
}
