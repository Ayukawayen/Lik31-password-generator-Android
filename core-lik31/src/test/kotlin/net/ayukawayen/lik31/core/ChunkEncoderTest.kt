package net.ayukawayen.lik31.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkEncoderTest {
    @Test
    fun groupMaxIs135000() {
        assertEquals(135000, Charset.GROUP_MAX)
    }

    @Test
    fun encodeGroupBoundaries() {
        assertEquals("Bab00", ChunkEncoder.encodeGroup(0))
        assertEquals("Bab01", ChunkEncoder.encodeGroup(1))
        assertEquals("Bab10", ChunkEncoder.encodeGroup(10))
        assertEquals("Bad00", ChunkEncoder.encodeGroup(100))
        assertEquals("Zuz99", ChunkEncoder.encodeGroup(Charset.GROUP_MAX - 1))
    }

    @Test
    fun thirtyTwoBytesGiveFifteenGroups() {
        val out = ChunkEncoder.encode(ByteArray(32))
        assertEquals(Lik31.RAW_LENGTH, out.length)
    }

    @Test
    fun allZeroBytesAlternatePlainAndMirrored() {
        // flag = 0: rightmost group (index 0) is plain, the next is mirrored, ...
        // 15 groups, so the leftmost (index 14) is plain again.
        val out = ChunkEncoder.encode(ByteArray(32))
        val groups = out.chunked(5)
        assertEquals("Bab00", groups.first())
        assertEquals("Bab00", groups.last())
        assertEquals("Zuz99", groups[1])
        assertEquals("Zuz99", groups[13])
    }

    @Test
    fun topBitOfFirstByteFlipsWhichGroupsAreMirrored() {
        val bytes = ByteArray(32)
        bytes[0] = 0x80.toByte() // only the flag bit set; it is not part of any group
        val groups = ChunkEncoder.encode(bytes).chunked(5)
        assertEquals("Zuz99", groups.first())
        assertEquals("Zuz99", groups.last())
        assertEquals("Bab00", groups[1])
    }

    @Test
    fun highBytesAreTreatedAsUnsigned() {
        // 0xFF bytes: every 17-bit group is 0x1FFFF = 131071.
        val bytes = ByteArray(32) { 0xFF.toByte() }
        val groups = ChunkEncoder.encode(bytes).chunked(5)
        val plain = ChunkEncoder.encodeGroup(131071)
        val mirrored = ChunkEncoder.encodeGroup(Charset.GROUP_MAX - 1 - 131071)
        assertNotEquals(plain, mirrored)
        // flag = 1 -> rightmost group mirrored
        assertEquals(mirrored, groups.last())
        assertEquals(plain, groups[13])
        // A sign-extension bug would collapse the output into two repeating groups
        // that differ from these.
        assertTrue(groups.all { it == plain || it == mirrored })
    }

    @Test
    fun everyGroupFollowsTheCharacterClassPattern() {
        val bytes = ByteArray(32) { (it * 37 + 11).toByte() }
        for (g in ChunkEncoder.encode(bytes).chunked(5)) {
            for (p in 0 until 5) assertTrue("$g[$p]", g[p] in Charset.CLASSES[p])
        }
    }
}
