package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.settings.AccountMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncDatesTest {
    @Test
    fun startDateWinsOverLastPosted() {
        val mapping = AccountMapping(1, "u", "2026-08-01", "2026-08-28")
        assertEquals("2026-08-01", SyncEngine.fetchFromDate(mapping))
    }

    @Test
    fun overlapFromLastPostedWhenStartBlank() {
        val mapping = AccountMapping(1, "u", null, "2026-03-10")
        assertEquals("2026-03-03", SyncEngine.fetchFromDate(mapping))
    }

    @Test
    fun openPendingPullsFetchEarlierThanFrom() {
        val mapping = AccountMapping(1, "u", "2026-08-22", "2026-08-29")
        assertEquals("2026-08-09", SyncEngine.fetchFromDate(mapping, "2026-08-10"))
    }

    @Test
    fun firstSyncUsesStartDate() {
        val mapping = AccountMapping(1, "u", "2026-03-01", null)
        assertEquals("2026-03-01", SyncEngine.fetchFromDate(mapping))
    }

    @Test
    fun blankStartMeansAllHistory() {
        val mapping = AccountMapping(1, "u", null, null)
        assertNull(SyncEngine.fetchFromDate(mapping))
    }

    @Test
    fun nextStartIsLastPostedMinusOverlap() {
        assertEquals("2026-08-22", AccountMapping.lookbackFloor("2026-08-29", null))
        assertNull(AccountMapping.lookbackFloor(null, null))
        assertNull(AccountMapping.lookbackFloor("  ", null))
        assertEquals("2026-08-09", AccountMapping.lookbackFloor("2026-08-29", "2026-08-10"))
    }

    @Test
    fun successfulImportDoesNotMoveStartBackwards() {
        val mapping = AccountMapping(1, "u", "2026-08-22", null)
        val next = mapping.afterSuccessfulImport("2026-08-29")
        assertEquals("2026-08-29", next.lastPostedDate)
        assertEquals("2026-08-22", next.syncStartDate)
    }

    @Test
    fun successfulImportRollsStartForwardOnly() {
        val mapping = AccountMapping(1, "u", "2026-01-01", null)
        val next = mapping.afterSuccessfulImport("2026-08-29")
        assertEquals("2026-08-22", next.syncStartDate)
    }

    @Test
    fun firstOfMonthWalksForwardOnceOverlapPassesIt() {
        val mapping = AccountMapping(1, "u", "2026-08-01", null)
        val next = mapping.afterSuccessfulImport("2026-08-29")
        assertEquals("2026-08-22", next.syncStartDate)
    }

    @Test
    fun blankStartGetsOverlapWindow() {
        val mapping = AccountMapping(1, "u", null, null)
        val next = mapping.afterSuccessfulImport("2026-08-29")
        assertEquals("2026-08-22", next.syncStartDate)
    }

    @Test
    fun persistDoesNotJumpBackForAnOpenHold() {
        val mapping = AccountMapping(1, "u", "2026-08-22", "2026-08-29")
        val next = mapping.afterSuccessfulImport("2026-08-29", "2026-08-10")
        assertEquals("2026-08-22", next.syncStartDate)
    }

    @Test
    fun noPostedLeavesStartAlone() {
        val mapping = AccountMapping(1, "u", "2026-08-01", null)
        val next = mapping.afterSuccessfulImport(null)
        assertEquals("2026-08-01", next.syncStartDate)
        assertNull(next.lastPostedDate)
    }

    @Test
    fun dateIntRoundTrip() {
        assertEquals(20260315, SyncEngine.isoToDateInt("2026-03-15"))
        assertEquals("2026-03-15", SyncEngine.dateIntToIso(20260315))
    }
}
