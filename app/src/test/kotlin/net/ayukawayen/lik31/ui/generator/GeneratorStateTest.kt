package net.ayukawayen.lik31.ui.generator

import net.ayukawayen.lik31.core.Lik31
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorStateTest {
    private fun GeneratorState.send(vararg events: GeneratorEvent): GeneratorState =
        events.fold(this) { s, e -> s.reduce(e) }

    private val typed = GeneratorState().send(
        GeneratorEvent.MainKeyChanged("correct horse"),
        GeneratorEvent.ServiceKeyChanged("github.com"),
    )

    @Test
    fun initialStateShowsNothing() {
        val s = GeneratorState()
        assertEquals("", s.password)
        assertFalse(s.canCopy)
        assertEquals(Lik31.DEFAULT_LENGTH, s.length)
    }

    @Test
    fun typingBothKeysShowsTheBrowserPassword() {
        assertEquals("Yey06Puf32Jix51Net95", typed.password)
        assertTrue(typed.canCopy)
    }

    @Test
    fun orderOfTypingDoesNotMatter() {
        val other = GeneratorState().send(
            GeneratorEvent.ServiceKeyChanged("github.com"),
            GeneratorEvent.MainKeyChanged("correct horse"),
        )
        assertEquals(typed.password, other.password)
    }

    @Test
    fun symbolAndLengthMatchMockup() {
        val s = typed.send(GeneratorEvent.SymbolChanged("!"), GeneratorEvent.LengthChanged(16))
        assertEquals("Yey06!Puf32Jix51", s.password)
    }

    @Test
    fun emptyMainKeyStillGeneratesLikeTheBrowser() {
        val s = GeneratorState().send(GeneratorEvent.ServiceKeyChanged("github.com"))
        assertEquals("Kup69Wes25Hid99Gig54", s.password)
    }

    @Test
    fun clearingMainKeyReturnsToEmptyKeyPassword() {
        val s = typed.send(GeneratorEvent.MainKeyChanged(""))
        assertEquals("Kup69Wes25Hid99Gig54", s.password)
    }

    @Test
    fun emptyServiceKeyShowsNoPassword() {
        val s = typed.send(GeneratorEvent.ServiceKeyChanged(""))
        assertEquals("", s.password)
        assertFalse(s.canCopy)
    }

    @Test
    fun serviceKeyIsCaseInsensitiveAndWhitespaceSensitive() {
        assertEquals(typed.password, typed.send(GeneratorEvent.ServiceKeyChanged("GitHub.com")).password)
        assertEquals(
            "Gaf37Kif65Pig48Doz52",
            typed.send(GeneratorEvent.ServiceKeyChanged("github.com ")).password,
        )
    }

    @Test
    fun lengthIsClamped() {
        assertEquals(6, typed.send(GeneratorEvent.LengthChanged(1)).length)
        assertEquals(75, typed.send(GeneratorEvent.LengthChanged(999)).length)
        assertEquals(75, typed.send(GeneratorEvent.LengthChanged(75)).password.length)
        assertEquals(6, typed.send(GeneratorEvent.LengthChanged(6)).password.length)
    }

    @Test
    fun symbolKeepsOnlyOneCharacterAndTypingReplacesIt() {
        assertEquals("!", sanitizeSymbol("!", ""))
        assertEquals("@", sanitizeSymbol("!@", "!"))
        assertEquals("", sanitizeSymbol("", "!"))
        assertEquals(" ", sanitizeSymbol(" ", ""))
    }

    @Test
    fun symbolRejectsEmojiAndControlCharacters() {
        assertEquals("", sanitizeSymbol("😀", ""))
        assertEquals("!", sanitizeSymbol("!😀", "!"))
        assertEquals("#", sanitizeSymbol("😀", "#"))
        assertEquals("", sanitizeSymbol("\n", ""))
        val s = typed.send(GeneratorEvent.SymbolChanged("😀"))
        assertEquals(typed.password, s.password)
    }

    @Test
    fun toggleVisibilityDoesNotChangePassword() {
        val s = typed.send(GeneratorEvent.ToggleMainKeyVisibility)
        assertTrue(s.mainKeyVisible)
        assertEquals(typed.password, s.password)
    }

    @Test
    fun toStringDoesNotLeakSecrets() {
        val text = typed.toString()
        assertFalse(text.contains("correct horse"))
        assertFalse(text.contains(typed.mainHash))
        assertFalse(text.contains(typed.password))
        assertFalse(text.contains(typed.raw))
    }
}
