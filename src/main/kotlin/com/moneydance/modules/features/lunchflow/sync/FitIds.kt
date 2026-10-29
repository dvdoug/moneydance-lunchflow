package com.moneydance.modules.features.lunchflow.sync

import com.infinitekind.moneydance.model.OnlineTxn
import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import java.security.MessageDigest

object FitIds {
    const val PROTOCOL: Int = OnlineTxn.PROTO_TYPE_OFX
    const val PREFIX_POSTED: String = "lunchflow:"
    const val PREFIX_PENDING: String = "lunchflow:pending:"

    fun posted(accountId: Long, txnId: String): String = "$PREFIX_POSTED$accountId:$txnId"

    fun pendingKey(accountId: Long, txn: LunchFlowTransaction): String {
        val id = txn.id?.takeIf { it.isNotBlank() }
        return if (id != null) {
            "$PREFIX_PENDING$accountId:$id"
        } else {
            "$PREFIX_PENDING$accountId:synth:${synthHash(txn)}"
        }
    }

    fun isPending(fitId: String?): Boolean {
        val v = fitId ?: return false
        return v.startsWith(PREFIX_PENDING)
    }

    fun isPosted(fitId: String?): Boolean {
        val v = fitId ?: return false
        return v.startsWith(PREFIX_POSTED) && !isPending(v)
    }

    fun isOurs(fitId: String?): Boolean = isPosted(fitId) || isPending(fitId)

    /**
     * Real Lunch Flow id inside `lunchflow:pending:{accountId}:{id}`.
     * Synthetic holds (`synth:…`) have no posted id to follow.
     */
    fun pendingTransactionId(fitId: String?): String? {
        if (!isPending(fitId)) return null
        val rest = fitId!!.removePrefix(PREFIX_PENDING)
        val splitAt = rest.indexOf(':')
        if (splitAt <= 0 || splitAt >= rest.length - 1) return null
        val tail = rest.substring(splitAt + 1)
        if (tail.startsWith("synth:")) return null
        return tail.takeIf { it.isNotBlank() }
    }

    fun synthHash(txn: LunchFlowTransaction): String {
        val raw = listOf(
            txn.date,
            txn.amount.toString(),
            txn.currency ?: "",
            txn.merchant?.trim().orEmpty(),
            txn.description?.trim().orEmpty()
        ).joinToString("\u0001")
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }.take(16)
    }
}
