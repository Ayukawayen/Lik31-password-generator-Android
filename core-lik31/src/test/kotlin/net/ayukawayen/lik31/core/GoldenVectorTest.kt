package net.ayukawayen.lik31.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compares every stage of the algorithm with values produced by the browser
 * extension's own code (see tools/gen-vectors).
 */
class GoldenVectorTest {
    private data class Vector(
        val m: String, val s: String, val sym: String, val len: Int,
        val mh: String, val sn: String, val raw: String, val pw: String,
    )

    private fun load(): List<Vector> {
        val stream = checkNotNull(javaClass.getResourceAsStream("/golden-vectors.json")) {
            "golden-vectors.json missing; run: node tools/gen-vectors/gen-vectors.mjs"
        }
        val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        @Suppress("UNCHECKED_CAST")
        val list = MiniJson(text).parse() as List<Map<String, Any?>>
        return list.map {
            Vector(
                it["m"] as String, it["s"] as String, it["sym"] as String, it["len"] as Int,
                it["mh"] as String, it["sn"] as String, it["raw"] as String, it["pw"] as String,
            )
        }
    }

    private fun describe(i: Int, v: Vector): String =
        "vector #$i m=${escape(v.m)} s=${escape(v.s)} sym=${escape(v.sym)} len=${v.len}"

    private fun escape(s: String): String =
        s.map { if (it.code in 0x20..0x7E) it.toString() else "\\u%04x".format(it.code) }.joinToString("", "\"", "\"")

    @Test
    fun fileHasEnoughVectors() {
        assertTrue(load().size >= 200)
    }

    @Test
    fun mainKeyHashMatchesBrowser() {
        load().forEachIndexed { i, v -> assertEquals(describe(i, v), v.mh, Lik31.hashMainKey(v.m)) }
    }

    @Test
    fun serviceKeyNormalizationMatchesBrowser() {
        load().forEachIndexed { i, v -> assertEquals(describe(i, v), v.sn, Lik31.normalizeServiceKey(v.s)) }
    }

    @Test
    fun rawPasswordMatchesBrowser() {
        load().forEachIndexed { i, v ->
            assertEquals(describe(i, v), v.raw, Lik31.rawPassword(v.mh, v.sn))
        }
    }

    @Test
    fun wrappedPasswordMatchesBrowser() {
        load().forEachIndexed { i, v -> assertEquals(describe(i, v), v.pw, Lik31.wrap(v.raw, v.sym, v.len)) }
    }

    @Test
    fun endToEndMatchesBrowser() {
        load().forEachIndexed { i, v ->
            assertEquals(describe(i, v), v.pw, Lik31.generate(v.m, v.s, v.sym, v.len))
        }
    }
}
