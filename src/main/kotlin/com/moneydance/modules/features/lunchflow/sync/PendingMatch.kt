package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class MatchPair(
    val pendingKey: String,
    val posted: LunchFlowTransaction
)

object PendingMatch {
    const val MAX_DAY_GAP: Long = 7

    fun uniquePairs(
        droppedPending: List<Pair<String, LunchFlowTransaction>>,
        newPosted: List<LunchFlowTransaction>
    ): List<MatchPair> {
        val usedPending = mutableSetOf<String>()
        val usedPosted = mutableSetOf<String>()
        val pairs = mutableListOf<MatchPair>()
        for ((pkey, pending) in droppedPending) {
            val matches = newPosted.filter { posted ->
                val pid = posted.id ?: return@filter false
                pid !in usedPosted && matches(pending, posted)
            }
            if (matches.size != 1) continue
            val posted = matches[0]
            val reverse = droppedPending.count { (otherKey, other) ->
                otherKey !in usedPending && matches(other, posted)
            }
            if (reverse != 1) continue
            usedPending.add(pkey)
            usedPosted.add(posted.id!!)
            pairs.add(MatchPair(pkey, posted))
        }
        return pairs
    }

    fun matches(pending: LunchFlowTransaction, posted: LunchFlowTransaction): Boolean {
        if (pending.amount != posted.amount) return false
        val pCur = pending.currency?.trim().orEmpty()
        val sCur = posted.currency?.trim().orEmpty()
        if (pCur.isNotEmpty() && sCur.isNotEmpty() && !pCur.equals(sCur, ignoreCase = true)) return false
        if (!payeeKey(pending).equals(payeeKey(posted), ignoreCase = true)) return false
        val days = abs(ChronoUnit.DAYS.between(parseDate(pending.date), parseDate(posted.date)))
        return days <= MAX_DAY_GAP
    }

    private fun payeeKey(txn: LunchFlowTransaction): String {
        val m = txn.merchant?.trim().orEmpty()
        if (m.isNotEmpty()) return m
        return txn.description?.trim().orEmpty()
    }

    private fun parseDate(iso: String): LocalDate = LocalDate.parse(iso.take(10))
}
