package net.ayukawayen.lik31.ui.generator

import net.ayukawayen.lik31.core.Lik31
import net.ayukawayen.lik31.data.ServiceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // ---- remembered service settings (phase 4) ----

    private fun service(value: String, saved: ServiceConfig? = null) = GeneratorEvent.ServiceKeyChanged(value, saved)

    @Test
    fun knownServiceRestoresLengthAndSymbol() {
        val s = GeneratorState().send(
            GeneratorEvent.MainKeyChanged("correct horse"),
            service("google.com", ServiceConfig(14, "@")),
        )
        assertEquals(14, s.length)
        assertEquals("@", s.symbol)
        assertTrue(s.configApplied)
        assertEquals("Tas57@Fug72Wer", s.password)
    }

    @Test
    fun unknownServiceUsesLastHandPickedSettings() {
        val s = GeneratorState().send(
            GeneratorEvent.LengthChanged(30),
            GeneratorEvent.SymbolChanged("#"),
            service("goo"),
        )
        assertEquals(30, s.length)
        assertEquals("#", s.symbol)
        assertFalse(s.configApplied)
    }

    @Test
    fun typingPastAKnownServiceRevertsToHandPickedSettings() {
        val s = GeneratorState().send(
            service("goo"),
            service("google.com", ServiceConfig(14, "@")),
            service("google.com.tw"),
        )
        assertEquals(Lik31.DEFAULT_LENGTH, s.length)
        assertEquals("", s.symbol)
        assertFalse(s.configApplied)
    }

    @Test
    fun editingAfterRestoreClearsTheAppliedHintAndBecomesTheNewFallback() {
        val s = GeneratorState().send(
            service("google.com", ServiceConfig(14, "@")),
            GeneratorEvent.LengthChanged(18),
        )
        assertFalse(s.configApplied)
        assertEquals("@", s.symbol)
        val next = s.send(service("other"))
        assertEquals(18, next.length)
        assertEquals("", next.symbol)
    }

    @Test
    fun corruptRememberedValuesAreRepaired() {
        val s = GeneratorState().send(service("x", ServiceConfig(500, "😀")))
        assertEquals(75, s.length)
        assertEquals("", s.symbol)
    }

    @Test
    fun configToRememberNeedsAPassword() {
        assertNull(GeneratorState().configToRemember)
        val s = typed.send(GeneratorEvent.SymbolChanged("!"), GeneratorEvent.LengthChanged(16))
        assertEquals(ServiceConfig(16, "!"), s.configToRemember)
        assertEquals("github.com", typed.send(service("GitHub.com")).normalizedServiceKey)
    }

    // ---- saved main key (phase 5) ----

    private val savedHash = Lik31.hashMainKey("correct horse")
    private val restored = GeneratorState().send(GeneratorEvent.SavedHashLoaded(savedHash))

    @Test
    fun savedHashIsUsedWhileMainKeyFieldIsEmpty() {
        assertTrue(restored.saveMainKey)
        assertTrue(restored.hasSavedMainKey)
        assertEquals("Yey06Puf32Jix51Net95", restored.send(service("github.com")).password)
    }

    @Test
    fun nothingSavedLeavesSwitchOff() {
        val s = GeneratorState().send(GeneratorEvent.SavedHashLoaded(null))
        assertFalse(s.saveMainKey)
        assertFalse(s.hasSavedMainKey)
        assertEquals(GeneratorState.EMPTY_MAIN_HASH, s.mainHash)
    }

    @Test
    fun typingOverridesSavedKeyAndClearingReturnsToIt() {
        val typedOther = restored.send(service("github.com"), GeneratorEvent.MainKeyChanged("other"))
        assertEquals(Lik31.generate("other", "github.com", "", 20), typedOther.password)
        assertTrue(typedOther.shouldPersistMainKey)
        val cleared = typedOther.send(GeneratorEvent.MainKeyChanged(""))
        assertEquals("Yey06Puf32Jix51Net95", cleared.password)
        assertFalse(cleared.shouldPersistMainKey)
    }

    @Test
    fun retypingTheSavedKeyDoesNotNeedPersisting() {
        assertFalse(restored.send(GeneratorEvent.MainKeyChanged("correct horse")).shouldPersistMainKey)
    }

    @Test
    fun switchOnWithTypedKeyPersistsAndRecordsIt() {
        val s = GeneratorState().send(
            GeneratorEvent.MainKeyChanged("correct horse"),
            GeneratorEvent.SaveMainKeyToggled(true),
        )
        assertTrue(s.shouldPersistMainKey)
        val done = s.send(GeneratorEvent.MainKeyPersisted)
        assertEquals(savedHash, done.savedHash)
        assertFalse(done.shouldPersistMainKey)
    }

    @Test
    fun emptyMainKeyIsNeverPersisted() {
        val s = GeneratorState().send(GeneratorEvent.SaveMainKeyToggled(true), service("github.com"))
        assertTrue(s.saveMainKey)
        assertFalse(s.shouldPersistMainKey)
    }

    @Test
    fun switchOffWithNothingSavedJustTurnsOff() {
        val s = GeneratorState().send(GeneratorEvent.SaveMainKeyToggled(true), GeneratorEvent.SaveMainKeyToggled(false))
        assertFalse(s.saveMainKey)
        assertFalse(s.confirmDisableSave)
    }

    @Test
    fun switchOffWithSavedKeyAsksFirst() {
        val asking = restored.send(GeneratorEvent.SaveMainKeyToggled(false))
        assertTrue(asking.confirmDisableSave)
        assertTrue(asking.saveMainKey)
        assertTrue(asking.hasSavedMainKey)

        val cancelled = asking.send(GeneratorEvent.DisableSaveCancelled)
        assertFalse(cancelled.confirmDisableSave)
        assertTrue(cancelled.hasSavedMainKey)

        val confirmed = asking.send(service("github.com"), GeneratorEvent.DisableSaveConfirmed)
        assertFalse(confirmed.confirmDisableSave)
        assertFalse(confirmed.saveMainKey)
        assertFalse(confirmed.hasSavedMainKey)
        // back to the (empty) typed main key
        assertEquals("Kup69Wes25Hid99Gig54", confirmed.password)
    }

    @Test
    fun persistFailureKeepsSwitchOnlyIfAnOlderKeyExists() {
        val fresh = GeneratorState().send(
            GeneratorEvent.MainKeyChanged("k"),
            GeneratorEvent.SaveMainKeyToggled(true),
            GeneratorEvent.MainKeyPersistFailed,
        )
        assertFalse(fresh.saveMainKey)
        assertTrue(fresh.saveFailed)

        val replacing = restored.send(GeneratorEvent.MainKeyChanged("k"), GeneratorEvent.MainKeyPersistFailed)
        assertTrue(replacing.saveMainKey)
        assertTrue(replacing.saveFailed)
        assertEquals(savedHash, replacing.savedHash)
    }

    @Test
    fun toStringDoesNotLeakSavedHash() {
        assertFalse(restored.toString().contains(savedHash))
    }

    // ---- main key field presentation ----

    @Test
    fun mainKeyModes() {
        assertEquals(MainKeyMode.Normal, GeneratorState().mainKeyMode)
        assertEquals(MainKeyMode.Normal, typed.mainKeyMode)
        assertEquals(MainKeyMode.UsingSaved, restored.mainKeyMode)
        assertEquals(MainKeyMode.RetypedSaved, restored.send(GeneratorEvent.MainKeyChanged("correct horse")).mainKeyMode)
        assertEquals(MainKeyMode.ReplacingSaved, restored.send(GeneratorEvent.MainKeyChanged("other")).mainKeyMode)
        assertEquals(
            MainKeyMode.UsingSaved,
            restored.send(GeneratorEvent.MainKeyChanged("other"), GeneratorEvent.MainKeyChanged("")).mainKeyMode,
        )
    }

    @Test
    fun savedKeyCannotBeRevealedButTypedTextCan() {
        assertTrue(GeneratorState().canRevealMainKey)
        assertFalse(restored.canRevealMainKey)
        assertTrue(restored.send(GeneratorEvent.MainKeyChanged("x")).canRevealMainKey)
    }

    @Test
    fun pendingBadgeFollowsWhatWouldBeSavedOnCopy() {
        assertFalse(typed.mainKeySavePending)
        assertTrue(typed.send(GeneratorEvent.SaveMainKeyToggled(true)).mainKeySavePending)
        assertFalse(restored.mainKeySavePending)
        assertTrue(restored.send(GeneratorEvent.MainKeyChanged("other")).mainKeySavePending)
        assertFalse(restored.send(GeneratorEvent.MainKeyChanged("correct horse")).mainKeySavePending)
    }

    @Test
    fun saveFailureShowsUntilTheNextEdit() {
        val failed = GeneratorState().send(
            GeneratorEvent.MainKeyChanged("k"),
            GeneratorEvent.SaveMainKeyToggled(true),
            GeneratorEvent.MainKeyPersistFailed,
        )
        assertEquals(MainKeyMode.SaveFailed, failed.mainKeyMode)
        assertEquals(MainKeyMode.Normal, failed.send(GeneratorEvent.MainKeyChanged("k2")).mainKeyMode)
    }

    // ---- biometric lock (phase 6) ----

    private val locked = GeneratorState().send(GeneratorEvent.SavedKeyLocked)

    @Test
    fun lockedKeyGeneratesNothingUntilUnlocked() {
        val s = locked.send(service("github.com"))
        assertTrue(s.savedLocked)
        assertTrue(s.hasSavedMainKey)
        assertTrue(s.saveMainKey)
        assertEquals("", s.password)
        assertFalse(s.canCopy)
        assertEquals(MainKeyMode.Locked, s.mainKeyMode)
        assertFalse(s.canRevealMainKey)

        val unlocked = s.send(GeneratorEvent.SavedHashLoaded(savedHash))
        assertFalse(unlocked.savedLocked)
        assertEquals("Yey06Puf32Jix51Net95", unlocked.password)
        assertEquals(MainKeyMode.UsingSaved, unlocked.mainKeyMode)
    }

    @Test
    fun typingWhileLockedWorksForThisSessionButIsNotStored() {
        val s = locked.send(service("github.com"), GeneratorEvent.MainKeyChanged("correct horse"))
        assertEquals("Yey06Puf32Jix51Net95", s.password)
        assertEquals(MainKeyMode.LockedTyped, s.mainKeyMode)
        assertTrue(s.canRevealMainKey)
        assertFalse(s.shouldPersistMainKey)
        assertFalse(s.mainKeySavePending)
        // clearing goes back to "locked, nothing to show"
        assertEquals("", s.send(GeneratorEvent.MainKeyChanged("")).password)
    }

    @Test
    fun unlockingAfterTypingFollowsTheNormalSavedRules() {
        val same = locked.send(GeneratorEvent.MainKeyChanged("correct horse"), GeneratorEvent.SavedHashLoaded(savedHash))
        assertEquals(MainKeyMode.RetypedSaved, same.mainKeyMode)
        val other = locked.send(GeneratorEvent.MainKeyChanged("other"), GeneratorEvent.SavedHashLoaded(savedHash))
        assertEquals(MainKeyMode.ReplacingSaved, other.mainKeyMode)
        assertTrue(other.shouldPersistMainKey)
    }

    @Test
    fun switchingOffWhileLockedStillAsksAndThenClears() {
        val asking = locked.send(GeneratorEvent.SaveMainKeyToggled(false))
        assertTrue(asking.confirmDisableSave)
        val done = asking.send(GeneratorEvent.DisableSaveConfirmed)
        assertFalse(done.hasSavedMainKey)
        assertFalse(done.savedLocked)
        assertFalse(done.saveMainKey)
        assertEquals(MainKeyMode.Normal, done.mainKeyMode)
    }

    @Test
    fun invalidatedKeyAsksForRetypeUntilSomethingIsTyped() {
        val lost = locked.send(service("github.com"), GeneratorEvent.SavedKeyInvalidated)
        assertFalse(lost.hasSavedMainKey)
        assertFalse(lost.saveMainKey)
        assertEquals(MainKeyMode.SavedLost, lost.mainKeyMode)
        assertEquals(MainKeyMode.Normal, lost.send(GeneratorEvent.MainKeyChanged("k")).mainKeyMode)
    }

    @Test
    fun toStringDoesNotLeakWhileLocked() {
        val s = locked.send(GeneratorEvent.MainKeyChanged("correct horse"), service("github.com"))
        assertFalse(s.toString().contains("correct horse"))
        assertFalse(s.toString().contains(s.password))
    }

    // ---- warnings (phase 7) ----

    @Test
    fun emptyMainKeyWarningOnlyWhenAPasswordComesFromIt() {
        assertFalse(GeneratorState().mainKeyEmptyWarning)
        val s = GeneratorState().send(service("github.com"))
        assertTrue(s.mainKeyEmptyWarning)
        assertEquals(MainKeyMode.EmptyWarning, s.mainKeyMode)
        assertEquals("Kup69Wes25Hid99Gig54", s.password)
        assertFalse(typed.mainKeyEmptyWarning)
        assertFalse(restored.send(service("github.com")).mainKeyEmptyWarning)
        assertFalse(locked.send(service("github.com")).mainKeyEmptyWarning)
    }

    @Test
    fun edgeWhitespaceWarning() {
        assertFalse(typed.serviceKeyEdgeWhitespace)
        assertFalse(GeneratorState().serviceKeyEdgeWhitespace)
        assertTrue(typed.send(service("github.com ")).serviceKeyEdgeWhitespace)
        assertTrue(typed.send(service(" github.com")).serviceKeyEdgeWhitespace)
        assertTrue(typed.send(service("github.com\u3000")).serviceKeyEdgeWhitespace)
        assertTrue(typed.send(service("github.com\t")).serviceKeyEdgeWhitespace)
        assertFalse(typed.send(service("my bank")).serviceKeyEdgeWhitespace)
        // the warning never blocks: the password is the browser's for that exact key
        assertEquals("Gaf37Kif65Pig48Doz52", typed.send(service("github.com ")).password)
    }

    @Test
    fun weakLengthWarning() {
        assertFalse(typed.lengthIsWeak)
        assertTrue(typed.send(GeneratorEvent.LengthChanged(12)).lengthIsWeak)
        assertTrue(typed.send(GeneratorEvent.LengthChanged(6)).lengthIsWeak)
        assertFalse(typed.send(GeneratorEvent.LengthChanged(13)).lengthIsWeak)
        assertTrue(typed.send(service("x", ServiceConfig(8, ""))).lengthIsWeak)
    }
}
