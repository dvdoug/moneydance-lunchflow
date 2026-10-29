package com.moneydance.modules.features.lunchflow.sync

import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class OpenHold(
    val key: String,
    val txn: LunchFlowTransaction,
    /** Confirmed, or already split. The register text belongs to the user. */
    val userOwned: Boolean
)

data class MatchPair(
    val pendingKey: String,
    val posted: LunchFlowTransaction,
    /** Leave description, memo, and splits. The FITID still moves to the settled item. */
    val keepRegisterText: Boolean = false
)

object PendingMatch {
    const val MAX_DAY_GAP: Long = 7

    fun uniquePairs(
        droppedPending: List<OpenHold>,
        newPosted: List<LunchFlowTransaction>
    ): List<MatchPair> {
        val usedPending = mutableSetOf<String>()
        val usedPosted = mutableSetOf<String>()
        val pairs = mutableListOf<MatchPair>()
        // Same Lunch Flow id once it posts, even when Merge kept the reminder name.
        collect(droppedPending, newPosted, usedPending, usedPosted, pairs, Mode.SAME_ID)
        collect(droppedPending, newPosted, usedPending, usedPosted, pairs, Mode.PAYEE)
        // Confirmed or split rows only: amount and date are unique, description is the user's.
        collect(droppedPending, newPosted, usedPending, usedPosted, pairs, Mode.USER_AMOUNT)
        return pairs
    }

    fun matches(pending: LunchFlowTransaction, posted: LunchFlowTransaction): Boolean =
        paired(pending, posted, Mode.PAYEE)

    private enum class Mode { SAME_ID, PAYEE, USER_AMOUNT }

    private fun collect(
        droppedPending: List<OpenHold>,
        newPosted: List<LunchFlowTransaction>,
        usedPending: MutableSet<String>,
        usedPosted: MutableSet<String>,
        pairs: MutableList<MatchPair>,
        mode: Mode
    ) {
        for (hold in droppedPending) {
            if (hold.key in usedPending) continue
            if (mode == Mode.USER_AMOUNT && !hold.userOwned) continue
            val pending = hold.txn
            val hits = newPosted.filter { posted ->
                val pid = posted.id ?: return@filter false
                pid !in usedPosted && paired(pending, posted, mode)
            }
            if (hits.size != 1) continue
            val posted = hits[0]
            val reverse = droppedPending.count { other ->
                other.key !in usedPending &&
                    (mode != Mode.USER_AMOUNT || other.userOwned) &&
                    paired(other.txn, posted, mode)
            }
            if (reverse != 1) continue
            usedPending.add(hold.key)
            usedPosted.add(posted.id!!)
            val keepText = hold.userOwned && !samePayee(pending, posted)
            pairs.add(MatchPair(hold.key, posted, keepText))
        }
    }

    private fun paired(pending: LunchFlowTransaction, posted: LunchFlowTransaction, mode: Mode): Boolean {
        if (mode == Mode.SAME_ID) {
            val left = pending.id
            val right = posted.id
            if (left.isNullOrBlank() || right.isNullOrBlank() || left != right) return false
        } else if (!withinDayGap(pending, posted)) {
            return false
        }
        if (mode == Mode.PAYEE && !samePayee(pending, posted)) return false
        if (!sameAmount(pending.amount, posted.amount)) return false
        return sameCurrency(pending, posted)
    }

    private fun sameAmount(a: Double, b: Double): Boolean = abs(abs(a) - abs(b)) < 0.001

    private fun sameCurrency(pending: LunchFlowTransaction, posted: LunchFlowTransaction): Boolean {
        val pCur = pending.currency?.trim().orEmpty()
        val sCur = posted.currency?.trim().orEmpty()
        return pCur.isEmpty() || sCur.isEmpty() || pCur.equals(sCur, ignoreCase = true)
    }

    private fun samePayee(pending: LunchFlowTransaction, posted: LunchFlowTransaction): Boolean =
        payeeKey(pending).equals(payeeKey(posted), ignoreCase = true)

    private fun withinDayGap(pending: LunchFlowTransaction, posted: LunchFlowTransaction): Boolean {
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
