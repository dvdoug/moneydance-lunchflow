package com.moneydance.modules.features.lunchflow.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingAmountTest {
    @Test
    fun sameMagnitudeIsNotAChange() {
        assertFalse(PendingAmount.changed(-5000, -5000))
        assertFalse(PendingAmount.changed(5000, -5000))
        assertFalse(PendingAmount.changed(-5000, 5000))
    }

    @Test
    fun differentMagnitudeIsAChange() {
        assertTrue(PendingAmount.changed(-5000, -4800))
        assertTrue(PendingAmount.changed(5000, -4800))
        assertTrue(PendingAmount.changed(0, -4800))
    }

    @Test
    fun keepsRegisterSignWhenRewriting() {
        assertEquals(-4800, PendingAmount.registerMinor(-5000, -4800))
        assertEquals(4800, PendingAmount.registerMinor(5000, -4800))
        assertEquals(-4800, PendingAmount.registerMinor(0, -4800))
        assertEquals(4800, PendingAmount.registerMinor(0, 4800))
    }
}
