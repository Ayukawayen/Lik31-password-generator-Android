package net.ayukawayen.lik31.core

/**
 * A few known answers (taken from the browser extension) that the app can
 * verify on the device it runs on. The full comparison lives in the unit
 * tests; this guards against a broken runtime (crypto provider, Unicode
 * tables) on one particular device.
 */
object SelfCheck {
    private class Case(val mainKey: String, val serviceKey: String, val symbol: String, val length: Int, val expected: String)

    private val CASES = listOf(
        Case("correct horse", "github.com", "", 20, "Yey06Puf32Jix51Net95"),
        Case("correct horse", "GitHub.com", "!", 16, "Yey06!Puf32Jix51"),
        Case("correct horse", "google.com", "@", 14, "Tas57@Fug72Wer"),
        Case("", "github.com", "", 20, "Kup69Wes25Hid99Gig54"),
        Case("correct horse", "github.com ", "", 20, "Gaf37Kif65Pig48Doz52"),
    )

    val caseCount: Int get() = CASES.size

    /** Number of cases that produce the expected password; equals [caseCount] when all is well. */
    fun passed(): Int = CASES.count {
        try {
            Lik31.generate(it.mainKey, it.serviceKey, it.symbol, it.length) == it.expected
        } catch (e: Exception) {
            false
        }
    }
}
