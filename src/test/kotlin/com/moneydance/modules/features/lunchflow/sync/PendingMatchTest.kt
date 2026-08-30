package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PendingMatchTest {
    private fun txn(
        id: String?,
        amount: Double,
        date: String,
        merchant: String,
        pending: Boolean
    ) = LunchFlowTransaction(id, 1, amount, "GBP", date, merchant, merchant, pending)

    @Test
    fun uniqueSameAmountMerchantDate() {
        val dropped = listOf(
            "pk" to txn(null, -10.0, "2026-03-01", "Shop", true)
        )
        val posted = listOf(txn("s1", -10.0, "2026-03-03", "Shop", false))
        val pairs = PendingMatch.uniquePairs(dropped, posted)
        assertEquals(1, pairs.size)
        assertEquals("s1", pairs[0].posted.id)
    }

    @Test
    fun amountChangeIsNotAMatch() {
        val dropped = listOf("pk" to txn(null, -10.0, "2026-03-01", "Shop", true))
        val posted = listOf(txn("s1", -12.0, "2026-03-01", "Shop", false))
        assertTrue(PendingMatch.uniquePairs(dropped, posted).isEmpty())
    }

    @Test
    fun twoIdenticalPendingAreAmbiguous() {
        val dropped = listOf(
            "a" to txn(null, -10.0, "2026-03-01", "Shop", true),
            "b" to txn(null, -10.0, "2026-03-01", "Shop", true)
        )
        val posted = listOf(txn("s1", -10.0, "2026-03-01", "Shop", false))
        assertTrue(PendingMatch.uniquePairs(dropped, posted).isEmpty())
    }
}
