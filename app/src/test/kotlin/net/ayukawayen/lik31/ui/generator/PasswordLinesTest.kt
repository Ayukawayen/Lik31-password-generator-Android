package net.ayukawayen.lik31.ui.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordLinesTest {
    /** Monospace model: advance = 0.6 em, in "dp" at font scale 1.15 (the reported phone). */
    private val scale = 1.15f
    private val mono: (Char, Int) -> Float = { _, sp -> 0.6f * sp * scale }

    /** 360dp screen: 360 - 88 = 272dp for the text. */
    private val width = 272f

    @Test
    fun firstLineIsFilled() {
        val lines = PasswordLines.wrap("Yey06Puf32Jix51Net95", width) { mono(it, 20) }
        // 0.6 * 20 * 1.15 = 13.8 -> 19 characters fit in 272
        assertEquals(listOf("Yey06Puf32Jix51Net9", "5"), lines)
    }

    @Test
    fun symbolsDoNotCauseEarlyBreaks() {
        for (sym in listOf("!", "?", "-", "/", " ", ".")) {
            val pw = "Yey06" + sym + "Puf32Jix51Net9"
            val lines = PasswordLines.wrap(pw, width) { mono(it, 20) }
            assertEquals(sym, 19, lines[0].length)
        }
    }

    @Test
    fun startsAtTwentySpWhenTwoLinesSuffice() {
        val layout = PasswordLines.fit("A".repeat(38), width, mono)
        assertEquals(20, layout.fontSizeSp)
        assertEquals(2, layout.lines.size)
    }

    @Test
    fun shrinksUntilTwoLinesFit() {
        val text = "A".repeat(50)
        val layout = PasswordLines.fit(text, width, mono)
        assertTrue(layout.fontSizeSp in 15..19)
        assertEquals(2, layout.lines.size)
        assertEquals(text, layout.lines.joinToString(""))
        // and one size larger would not have fitted
        val larger = PasswordLines.wrap(text, width) { mono(it, layout.fontSizeSp + 1) }
        assertTrue(larger.size > 2)
    }

    @Test
    fun allowsMoreLinesAtMinimumSize() {
        val text = "A".repeat(75)
        val layout = PasswordLines.fit(text, width, mono)
        assertEquals(PasswordLines.MIN_FONT_SP, layout.fontSizeSp)
        assertEquals(3, layout.lines.size)
        assertEquals(text, layout.lines.joinToString(""))
    }

    @Test
    fun wideSymbolIsMeasuredByItsOwnWidth() {
        // a full-width symbol takes two cells
        val w: (Char) -> Float = { if (it == '＠') 27.6f else 13.8f }
        val lines = PasswordLines.wrap("Yey06＠Puf32Jix51Net9", width, w)
        assertEquals(18, lines[0].length)
        lines.forEach { line -> assertTrue(line.sumOf { w(it).toDouble() } <= width) }
    }

    @Test
    fun everyLineHasAtLeastOneCharacterEvenWhenTooNarrow() {
        assertEquals(listOf("A", "B", "C"), PasswordLines.wrap("ABC", 1f) { 10f })
    }

    @Test
    fun emptyTextHasNoLines() {
        assertEquals(emptyList<String>(), PasswordLines.fit("", width, mono).lines)
    }
}
