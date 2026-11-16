package net.ayukawayen.lik31.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfCheckTest {
    @Test
    fun allBuiltInCasesPass() {
        assertTrue(SelfCheck.caseCount >= 5)
        assertEquals(SelfCheck.caseCount, SelfCheck.passed())
    }
}
