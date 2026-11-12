package net.ayukawayen.lik31.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

class Lik31Test {
    @Test
    fun documentedExamples() {
        assertEquals("Yey06Puf32Jix51Net95", Lik31.generate("correct horse", "github.com", "", 20))
        assertEquals("Yey06!Puf32Jix51", Lik31.generate("correct horse", "github.com", "!", 16))
        assertEquals("Tas57@Fug72Wer", Lik31.generate("correct horse", "google.com", "@", 14))
    }

    @Test
    fun emptyMainKeyHashIsSha256OfEmptyInput() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Lik31.hashMainKey(""),
        )
    }

    @Test
    fun serviceKeyCaseIsIgnoredButMainKeyCaseIsNot() {
        assertEquals(
            Lik31.generate("k", "GitHub.com", "", 20),
            Lik31.generate("k", "github.com", "", 20),
        )
        assertNotEquals(
            Lik31.generate("K", "github.com", "", 20),
            Lik31.generate("k", "github.com", "", 20),
        )
    }

    @Test
    fun whitespaceIsSignificant() {
        assertEquals("github.com ", Lik31.normalizeServiceKey("github.com "))
        assertNotEquals(
            Lik31.generate("k", "github.com ", "", 20),
            Lik31.generate("k", "github.com", "", 20),
        )
    }

    @Test
    fun serviceKeyIsNfcNormalizedButMainKeyIsNot() {
        assertEquals(Lik31.normalizeServiceKey("café"), Lik31.normalizeServiceKey("café"))
        assertNotEquals(Lik31.hashMainKey("café"), Lik31.hashMainKey("café"))
    }

    @Test
    fun lowercasingDoesNotDependOnDefaultLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("irmak", Lik31.normalizeServiceKey("IRMAK"))
            assertEquals("i̇stanbul", Lik31.normalizeServiceKey("İstanbul"))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun utf8EncodesLoneSurrogatesAsReplacementCharacter() {
        val fffd = byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte())
        assertArrayEquals(fffd, Utf8.encode("\ud800"))
        assertArrayEquals(fffd, Utf8.encode("\udc00"))
        assertArrayEquals(byteArrayOf('a'.code.toByte()) + fffd + byteArrayOf('b'.code.toByte()), Utf8.encode("a\ud83db"))
        // reversed pair = two lone surrogates
        assertArrayEquals(fffd + fffd, Utf8.encode("\udc00\ud800"))
    }

    @Test
    fun utf8MatchesThePlatformEncoderForWellFormedText() {
        for (s in listOf("", "abc", "é", "密碼", "😀", "a😀b密é", "\u0000\u007f\u0080߿ࠀ￿")) {
            assertArrayEquals(s, s.toByteArray(Charsets.UTF_8), Utf8.encode(s))
        }
    }

    @Test
    fun symbolIsInsertedAfterTheFirstGroupAndCountsTowardLength() {
        val raw = Lik31.rawPassword(Lik31.hashMainKey("k"), "x")
        assertEquals(raw.take(20), Lik31.wrap(raw, "", 20))
        assertEquals(raw.take(5) + "!" + raw.substring(5, 19), Lik31.wrap(raw, "!", 20))
        // shortest length always contains the symbol
        assertEquals(raw.take(5) + "!", Lik31.wrap(raw, "!", 6))
        // longest length with a symbol drops the last raw character
        assertEquals(raw.take(5) + "!" + raw.substring(5, 74), Lik31.wrap(raw, "!", 75))
        assertEquals(75, Lik31.wrap(raw, "!", 75).length)
    }

    @Test
    fun onlyFirstCodeUnitOfSymbolIsUsed() {
        val raw = Lik31.rawPassword(Lik31.hashMainKey("k"), "x")
        assertEquals(Lik31.wrap(raw, "!", 20), Lik31.wrap(raw, "!?", 20))
        assertEquals('\ud83d', Lik31.wrap(raw, "😀", 20)[5])
    }

    @Test
    fun wrapToleratesOutOfRangeLengths() {
        val raw = Lik31.rawPassword(Lik31.hashMainKey("k"), "x")
        assertEquals("", Lik31.wrap(raw, "", 0))
        assertEquals("", Lik31.wrap(raw, "", -3))
        assertEquals(raw, Lik31.wrap(raw, "", 500))
    }

    @Test
    fun clampLength() {
        assertEquals(6, Lik31.clampLength(-1))
        assertEquals(6, Lik31.clampLength(5))
        assertEquals(6, Lik31.clampLength(6))
        assertEquals(20, Lik31.clampLength(20))
        assertEquals(75, Lik31.clampLength(75))
        assertEquals(75, Lik31.clampLength(76))
    }
}
