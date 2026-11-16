package net.ayukawayen.lik31.ui.generator

/**
 * Decides how the password is broken into lines for display. Pure Kotlin so
 * it can be unit-tested; the UI supplies real glyph widths.
 *
 * The app breaks lines itself instead of letting the text engine do it,
 * because Unicode line breaking allows a break after some symbols ("!", "?",
 * "-", ...) but nowhere inside the letters and digits, which produced a first
 * line of only six characters.
 *
 * Rules: fill each line as far as it goes (first line fullest); start at
 * [MAX_FONT_SP] and shrink 1sp at a time until the password fits in
 * [TARGET_LINES] lines; at [MIN_FONT_SP] accept more lines.
 */
object PasswordLines {
    const val MAX_FONT_SP: Int = 18
    const val MIN_FONT_SP: Int = 14
    const val TARGET_LINES: Int = 2

    /** Line height as a multiple of the font size. */
    const val LINE_HEIGHT_FACTOR: Float = 1.2f

    data class Layout(val fontSizeSp: Int, val lines: List<String>)

    /**
     * @param availableWidth width a line may take, in the same unit as [charWidth]
     * @param charWidth advance width of a character at a font size in sp
     */
    fun fit(text: String, availableWidth: Float, charWidth: (Char, Int) -> Float): Layout {
        if (text.isEmpty()) return Layout(MAX_FONT_SP, emptyList())
        for (sp in MAX_FONT_SP downTo MIN_FONT_SP + 1) {
            val lines = wrap(text, availableWidth) { charWidth(it, sp) }
            if (lines.size <= TARGET_LINES) return Layout(sp, lines)
        }
        return Layout(MIN_FONT_SP, wrap(text, availableWidth) { charWidth(it, MIN_FONT_SP) })
    }

    /** Greedy wrap: each line takes as many characters as fit (at least one). */
    fun wrap(text: String, availableWidth: Float, charWidth: (Char) -> Float): List<String> {
        val lines = ArrayList<String>()
        val current = StringBuilder()
        var width = 0f
        for (c in text) {
            val w = charWidth(c)
            if (current.isNotEmpty() && width + w > availableWidth) {
                lines.add(current.toString())
                current.setLength(0)
                width = 0f
            }
            current.append(c)
            width += w
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
