package net.ayukawayen.lik31.core

/**
 * Tiny JSON reader for the golden vector file (arrays, objects, strings,
 * integers). Kept dependency-free on purpose; it preserves lone surrogates
 * written as \uXXXX escapes, which the vectors rely on.
 */
internal class MiniJson(private val src: String) {
    private var pos = 0

    fun parse(): Any? {
        val v = value()
        ws()
        check(pos == src.length) { "trailing data at $pos" }
        return v
    }

    private fun ws() {
        while (pos < src.length && src[pos].isWhitespace()) pos++
    }

    private fun value(): Any? {
        ws()
        return when (val c = src[pos]) {
            '[' -> array()
            '{' -> obj()
            '"' -> string()
            else -> if (c == '-' || c.isDigit()) number() else error("unexpected '$c' at $pos")
        }
    }

    private fun array(): List<Any?> {
        val out = ArrayList<Any?>()
        pos++
        ws()
        if (src[pos] == ']') { pos++; return out }
        while (true) {
            out.add(value())
            ws()
            when (src[pos++]) {
                ',' -> continue
                ']' -> return out
                else -> error("bad array at $pos")
            }
        }
    }

    private fun obj(): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        pos++
        ws()
        if (src[pos] == '}') { pos++; return out }
        while (true) {
            ws()
            val key = string()
            ws()
            check(src[pos++] == ':') { "expected ':' at $pos" }
            out[key] = value()
            ws()
            when (src[pos++]) {
                ',' -> continue
                '}' -> return out
                else -> error("bad object at $pos")
            }
        }
    }

    private fun string(): String {
        check(src[pos++] == '"')
        val sb = StringBuilder()
        while (true) {
            when (val c = src[pos++]) {
                '"' -> return sb.toString()
                '\\' -> when (val e = src[pos++]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'u' -> {
                        sb.append(src.substring(pos, pos + 4).toInt(16).toChar())
                        pos += 4
                    }
                    else -> sb.append(e)
                }
                else -> sb.append(c)
            }
        }
    }

    private fun number(): Int {
        val start = pos
        if (src[pos] == '-') pos++
        while (pos < src.length && src[pos].isDigit()) pos++
        return src.substring(start, pos).toInt()
    }
}
