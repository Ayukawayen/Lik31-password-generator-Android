package net.ayukawayen.lik31.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceConfigTest {
    @Test
    fun codecRoundTrips() {
        for (c in listOf(ServiceConfig(20, ""), ServiceConfig(14, "@"), ServiceConfig(75, ":"), ServiceConfig(6, " "), ServiceConfig(30, "＠"))) {
            assertEquals(c, ServiceConfigCodec.decode(ServiceConfigCodec.encode(c)))
        }
        assertEquals("14:@", ServiceConfigCodec.encode(ServiceConfig(14, "@")))
    }

    @Test
    fun codecRejectsMalformedText() {
        assertNull(ServiceConfigCodec.decode(""))
        assertNull(ServiceConfigCodec.decode("20"))
        assertNull(ServiceConfigCodec.decode(":@"))
        assertNull(ServiceConfigCodec.decode("abc:@"))
    }

    @Test
    fun codecRepairsOutOfRangeValues() {
        assertEquals(ServiceConfig(6, ""), ServiceConfigCodec.decode("1:"))
        assertEquals(ServiceConfig(75, "!"), ServiceConfigCodec.decode("999:!"))
        assertEquals(ServiceConfig(20, "!"), ServiceConfigCodec.decode("20:!?"))
        assertEquals(ServiceConfig(20, ""), ServiceConfigCodec.decode("20:😀"))
    }

    @Test
    fun indexIsStableAndDependsOnSaltAndKey() {
        val a = ServiceKeyIndex(ByteArray(32) { 1 })
        val b = ServiceKeyIndex(ByteArray(32) { 2 })
        assertEquals(a.storageKey("github.com"), a.storageKey("github.com"))
        assertNotEquals(a.storageKey("github.com"), a.storageKey("google.com"))
        assertNotEquals(a.storageKey("github.com"), b.storageKey("github.com"))
        val key = a.storageKey("github.com")
        assertTrue(key.startsWith("sc:"))
        assertEquals(3 + 64, key.length)
        assertFalse(key.contains("github"))
    }

    @Test
    fun indexMatchesAKnownHmacVector() {
        // RFC 4231 test case 2: key "Jefe", data "what do ya want for nothing?"
        val index = ServiceKeyIndex("Jefe".toByteArray())
        assertEquals(
            "sc:5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            index.storageKey("what do ya want for nothing?"),
        )
    }

    @Test
    fun hexRoundTripsAndRejectsGarbage() {
        val bytes = ByteArray(256) { it.toByte() }
        assertArrayEquals(bytes, Hex.decode(Hex.encode(bytes)))
        assertEquals("00ff10", Hex.encode(byteArrayOf(0, -1, 16)))
        assertNull(Hex.decode("abc"))
        assertNull(Hex.decode("zz"))
    }

    @Test
    fun vaultHashValidation() {
        assertTrue(MainKeyVault.isValidHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
        assertFalse(MainKeyVault.isValidHash(""))
        assertFalse(MainKeyVault.isValidHash("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855"))
        assertFalse(MainKeyVault.isValidHash("e3b0"))
    }
}
