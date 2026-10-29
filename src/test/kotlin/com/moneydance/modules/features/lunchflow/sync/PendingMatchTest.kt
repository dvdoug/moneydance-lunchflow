package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingMatchTest {
    private fun txn(
        id: String?,
        amount: Double,
        date: String,
        merchant: String,
        pending: Boolean
    ) = LunchFlowTransaction(id, 1, amount, "GBP", date, merchant, merchant, pending)

    private fun hold(key: String, txn: LunchFlowTransaction, userOwned: Boolean = false) =
        OpenHold(key, txn, userOwned)

    @Test
    fun uniqueSameAmountMerchantDate() {
        val dropped = listOf(hold("pk", txn(null, -10.0, "2026-03-01", "Shop", true)))
        val posted = listOf(txn("s1", -10.0, "2026-03-03", "Shop", false))
        val pairs = PendingMatch.uniquePairs(dropped, posted)
        assertEquals(1, pairs.size)
        assertEquals("s1", pairs[0].posted.id)
        assertFalse(pairs[0].keepRegisterText)
    }

    @Test
    fun amountChangeIsNotAMatch() {
        val dropped = listOf(hold("pk", txn("same", -10.0, "2026-03-01", "Shop", true), userOwned = true))
        val posted = listOf(txn("same", -12.0, "2026-03-01", "Shop", false))
        assertTrue(PendingMatch.uniquePairs(dropped, posted).isEmpty())
    }

    @Test
    fun twoIdenticalPendingAreAmbiguous() {
        val dropped = listOf(
            hold("a", txn(null, -10.0, "2026-03-01", "Shop", true)),
            hold("b", txn(null, -10.0, "2026-03-01", "Shop", true))
        )
        val posted = listOf(txn("s1", -10.0, "2026-03-01", "Shop", false))
        assertTrue(PendingMatch.uniquePairs(dropped, posted).isEmpty())
    }

    @Test
    fun signFlipStillMatchesMerchant() {
        val dropped = listOf(hold("pk", txn(null, 10.0, "2026-03-01", "Shop", true)))
        val posted = listOf(txn("s1", -10.0, "2026-03-02", "Shop", false))
        val pairs = PendingMatch.uniquePairs(dropped, posted)
        assertEquals(1, pairs.size)
        assertFalse(pairs[0].keepRegisterText)
    }

    @Test
    fun sameIdKeepsMergedReminder() {
        val dropped = listOf(hold("pk", txn("salary", 2500.0, "2026-10-27", "Salary", true), userOwned = true))
        val posted = listOf(
            txn("salary", 2500.0, "2026-10-27", "ACME LTD", false),
            txn("other", 2500.0, "2026-10-27", "ACME LTD", false)
        )
        val pairs = PendingMatch.uniquePairs(dropped, posted)
        assertEquals(1, pairs.size)
        assertEquals("salary", pairs[0].posted.id)
        assertTrue(pairs[0].keepRegisterText)
    }

    @Test
    fun syntheticHoldMatchesOnAmountOnlyWhenTheUserOwnsIt() {
        val owned = hold("pk", txn(null, -42.0, "2026-03-01", "Groceries", true), userOwned = true)
        val posted = listOf(txn("new-id", -42.0, "2026-03-02", "TESCO STORES", false))
        val pairs = PendingMatch.uniquePairs(listOf(owned), posted)
        assertEquals(1, pairs.size)
        assertEquals("new-id", pairs[0].posted.id)
        assertTrue(pairs[0].keepRegisterText)
    }

    @Test
    fun unconfirmedSyntheticHoldDoesNotAttachToADifferentMerchant() {
        val plain = hold("pk", txn(null, -4.5, "2026-03-01", "Cafe A", true))
        val posted = listOf(txn("new-id", -4.5, "2026-03-01", "Cafe B", false))
        assertTrue(PendingMatch.uniquePairs(listOf(plain), posted).isEmpty())
    }
}
