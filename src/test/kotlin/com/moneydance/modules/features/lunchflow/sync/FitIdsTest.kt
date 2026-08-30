package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitIdsTest {
    @Test
    fun postedAndPendingKeys() {
        val posted = LunchFlowTransaction("abc", 7, -1.0, "GBP", "2026-03-01", "A", "B", false)
        assertEquals("lunchflow:7:abc", FitIds.posted(7, "abc"))
        assertEquals("lunchflow:pending:7:abc", FitIds.pendingKey(7, posted))
        val pending = posted.copy(id = null, isPending = true)
        val key = FitIds.pendingKey(7, pending)
        assertTrue(key.startsWith("lunchflow:pending:7:synth:"))
        assertEquals(key, FitIds.pendingKey(7, pending))
    }

    @Test
    fun pendingLabelIsDisplayOnly() {
        assertEquals("TFL TRAVEL CHARGE", FitIds.stripPendingLabel("TFL TRAVEL CHARGE"))
        assertEquals("TFL TRAVEL CHARGE", FitIds.stripPendingLabel("[PENDING] TFL TRAVEL CHARGE"))
        assertEquals("[PENDING] TFL TRAVEL CHARGE", FitIds.withPendingLabel("TFL TRAVEL CHARGE"))
        assertEquals("[PENDING] TFL TRAVEL CHARGE", FitIds.withPendingLabel("[PENDING] TFL TRAVEL CHARGE"))
        assertEquals("ol.orig-payee", FitIds.ORIG_PAYEE_TAG)
    }

    @Test
    fun settledDescriptionKeepsConfirmedPayee() {
        assertEquals(
            "Tesco",
            FitIds.settledDescription("[PENDING] Tesco", "TESCO STORES 123", alreadyConfirmed = true)
        )
        assertEquals(
            "TESCO STORES 123",
            FitIds.settledDescription("[PENDING] Tesco", "TESCO STORES 123", alreadyConfirmed = false)
        )
        assertEquals(
            "Waitrose",
            FitIds.settledDescription("Waitrose", "WAITROSE 2844", alreadyConfirmed = true)
        )
    }
}
