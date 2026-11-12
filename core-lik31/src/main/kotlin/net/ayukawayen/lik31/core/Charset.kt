package net.ayukawayen.lik31.core

/**
 * The five character classes of one output group, in order:
 * upper-case consonant, vowel, lower-case consonant, digit, digit.
 * Must stay identical to CHARSET in the browser extension's popup.js.
 */
internal object Charset {
    val CLASSES: Array<String> = arrayOf(
        "BDFGHJKLMNPRSTVWYZ",
        "aeiou",
        "bdfgkmnprstvxyz",
        "0123456789",
        "0123456789",
    )

    /** Characters per group. */
    const val GROUP_LENGTH: Int = 5

    /** Number of distinct groups: 18 * 5 * 15 * 10 * 10 = 135000 (> 2^17). */
    val GROUP_MAX: Int = CLASSES.fold(1) { acc, c -> acc * c.length }
}
