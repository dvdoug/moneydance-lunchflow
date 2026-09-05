package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertTrue(FitIds.isPosted("lunchflow:7:abc"))
        assertFalse(FitIds.isPosted("lunchflow:pending:7:abc"))
        assertTrue(FitIds.isOurs("lunchflow:pending:7:abc"))
        assertTrue(FitIds.isOurs("lunchflow:7:abc"))
        assertFalse(FitIds.isOurs("ofx-other"))
    }

    @Test
    fun leftoverPendingPrefixStrips() {
        assertEquals("TFL TRAVEL CHARGE", FitIds.stripPendingLabel("TFL TRAVEL CHARGE"))
        assertEquals("TFL TRAVEL CHARGE", FitIds.stripPendingLabel("[PENDING] TFL TRAVEL CHARGE"))
        assertEquals("ol.orig-payee", FitIds.ORIG_PAYEE_TAG)
    }
}
