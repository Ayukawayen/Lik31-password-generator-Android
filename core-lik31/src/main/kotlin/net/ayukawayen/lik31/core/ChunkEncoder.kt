package net.ayukawayen.lik31.core

/**
 * Turns a hash into pronounceable groups such as "Yey06Puf32...".
 *
 * Bits are consumed from the END of the array, 17 at a time. A 32-byte hash
 * yields 15 groups (255 bits, 75 characters); the one bit left over is the top
 * bit of the first byte, which is used as [flag] to decide which groups are
 * mirrored (GROUP_MAX - 1 - v).
 */
internal object ChunkEncoder {
    private const val BITS_PER_GROUP = 17
    private const val GROUP_MASK = (1 shl BITS_PER_GROUP) - 1

    fun encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val flag = (bytes[0].toInt() and 0xFF) ushr 7

        // Groups are produced right-to-left; collect them and reverse at the end.
        val groups = ArrayList<String>(bytes.size * 8 / BITS_PER_GROUP)
        var value = 0
        var bitCount = 0
        for (i in bytes.indices.reversed()) {
            // Bytes are signed in Kotlin: mask to get the unsigned value.
            value = value or ((bytes[i].toInt() and 0xFF) shl bitCount)
            bitCount += 8
            if (bitCount >= BITS_PER_GROUP) {
                val v = value and GROUP_MASK
                // In the JS original the test is (result.length + flag) % 2 where
                // result.length is 5 * groupsSoFar; 5 is odd, so parity is the same.
                val mirrored = (groups.size + flag) % 2 != 0
                groups.add(encodeGroup(if (mirrored) Charset.GROUP_MAX - 1 - v else v))
                bitCount -= BITS_PER_GROUP
                value = value ushr BITS_PER_GROUP
            }
        }

        val sb = StringBuilder(groups.size * Charset.GROUP_LENGTH)
        for (g in groups.indices.reversed()) sb.append(groups[g])
        return sb.toString()
    }

    /** Mixed-radix encoding of [v] (0 until GROUP_MAX) into one 5-character group. */
    fun encodeGroup(v: Int): String {
        require(v in 0 until Charset.GROUP_MAX) { "group value out of range: $v" }
        val chars = CharArray(Charset.GROUP_LENGTH)
        var rest = v
        for (p in Charset.CLASSES.indices.reversed()) {
            val cls = Charset.CLASSES[p]
            chars[p] = cls[rest % cls.length]
            rest /= cls.length
        }
        return String(chars)
    }
}
