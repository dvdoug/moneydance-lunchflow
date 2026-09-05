package com.moneydance.modules.features.lunchflow.ui

import com.moneydance.modules.features.lunchflow.sync.AccountSyncResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportStatusTest {
    @Test
    fun skippedPostedAndOnePendingIsUpToDate() {
        val result = AccountSyncResult(postedSkipped = 49, pendingUpdated = 1)
        assertEquals(
            "Lloyds: up to date. 1 pending hold still open.",
            ImportStatus.line("Lloyds", result)
        )
    }

    @Test
    fun nothingChanged() {
        assertEquals(
            "Savings: up to date.",
            ImportStatus.line("Savings", AccountSyncResult(postedSkipped = 133))
        )
    }

    @Test
    fun newPosted() {
        assertEquals(
            "British Airways: added 3 transactions.",
            ImportStatus.line("British Airways", AccountSyncResult(postedAdded = 3))
        )
    }

    @Test
    fun oneNewPosted() {
        assertEquals(
            "Current: added 1 transaction.",
            ImportStatus.line("Current", AccountSyncResult(postedAdded = 1))
        )
    }

    @Test
    fun onlyNewPending() {
        assertEquals(
            "Current: added 1 new pending hold.",
            ImportStatus.line("Current", AccountSyncResult(pendingAdded = 1))
        )
    }

    @Test
    fun postedAndPendingTogether() {
        assertEquals(
            "Savings: added 2 transactions. 1 new pending hold.",
            ImportStatus.line("Savings", AccountSyncResult(postedAdded = 2, pendingAdded = 1))
        )
    }

    @Test
    fun overallUpToDate() {
        assertEquals(
            "up to date",
            ImportStatus.overall(listOf(AccountSyncResult(postedSkipped = 50)))
        )
    }

    @Test
    fun overallImportedAndError() {
        assertEquals(
            "imported 3 new transactions; 1 error",
            ImportStatus.overall(
                listOf(
                    AccountSyncResult(postedAdded = 3),
                    AccountSyncResult(error = "Currency mismatch")
                )
            )
        )
    }

    @Test
    fun pendingAmountUpdated() {
        assertEquals(
            "Lloyds: 1 pending hold updated.",
            ImportStatus.line("Lloyds", AccountSyncResult(pendingAdjusted = 1))
        )
    }

    @Test
    fun pendingUpdatedAndStillOpen() {
        assertEquals(
            "Lloyds: 1 pending hold updated. 2 pending holds still open.",
            ImportStatus.line("Lloyds", AccountSyncResult(pendingAdjusted = 1, pendingUpdated = 2))
        )
    }

    @Test
    fun overallPendingUpdated() {
        assertEquals(
            "1 pending hold updated",
            ImportStatus.overall(listOf(AccountSyncResult(pendingAdjusted = 1)))
        )
    }

    @Test
    fun errorWins() {
        assertEquals(
            "Current: Currency mismatch: Lunch Flow EUR vs Moneydance GBP.",
            ImportStatus.line(
                "Current",
                AccountSyncResult(error = "Currency mismatch: Lunch Flow EUR vs Moneydance GBP.")
            )
        )
    }
}
