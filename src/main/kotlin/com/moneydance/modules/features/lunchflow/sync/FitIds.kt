package com.moneydance.modules.features.lunchflow.sync

import com.infinitekind.moneydance.model.OnlineTxn
import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import java.security.MessageDigest

object FitIds {
    const val PROTOCOL: Int = OnlineTxn.PROTO_TYPE_OFX
    const val PARAM_PENDING: String = "lunchflow.pending"
    const val PREFIX_POSTED: String = "lunchflow:"
    const val PREFIX_PENDING: String = "lunchflow:pending:"
    const val PENDING_LABEL: String = "[PENDING] "
    /** Moneydance similar-payee tag. Must not include [PENDING] — the matcher is prefix-based. */
    const val ORIG_PAYEE_TAG: String = "ol.orig-payee"

    fun posted(accountId: Long, txnId: String): String = "$PREFIX_POSTED$accountId:$txnId"

    fun pendingKey(accountId: Long, txn: LunchFlowTransaction): String {
        val id = txn.id?.takeIf { it.isNotBlank() }
        return if (id != null) {
            "$PREFIX_PENDING$accountId:$id"
        } else {
            "$PREFIX_PENDING$accountId:synth:${synthHash(txn)}"
        }
    }

    fun isOurs(fitId: String?): Boolean {
        val v = fitId ?: return false
        return v.startsWith(PREFIX_POSTED)
    }

    fun isPending(fitId: String?): Boolean {
        val v = fitId ?: return false
        return v.startsWith(PREFIX_PENDING)
    }

    fun stripPendingLabel(text: String): String = text.removePrefix(PENDING_LABEL)

    fun withPendingLabel(text: String): String = PENDING_LABEL + stripPendingLabel(text)

    /** Confirmed rows keep the user's Description minus our label. Unconfirmed take the settled payee. */
    fun settledDescription(current: String, postedPayee: String, alreadyConfirmed: Boolean): String {
        if (!alreadyConfirmed) return postedPayee
        return stripPendingLabel(current).ifBlank { postedPayee }
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
