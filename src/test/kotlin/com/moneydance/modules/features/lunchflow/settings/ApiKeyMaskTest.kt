package com.moneydance.modules.features.lunchflow.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiKeyMaskTest {
    @Test
    fun showsLastFourOnly() {
        assertEquals("••••wxyz", ApiKeyMask.lastFour("abcdefghijklmnopwxyz"))
    }

    @Test
    fun shortKeysAreFullyMasked() {
        assertEquals("••••", ApiKeyMask.lastFour("ab"))
    }
}
