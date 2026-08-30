package com.moneydance.modules.features.lunchflow.ui

import com.moneydance.modules.features.lunchflow.sync.AccountSyncResult

object ImportStatus {
    fun line(accountName: String, result: AccountSyncResult): String {
        if (result.error != null) return "$accountName: ${result.error}"

        val parts = mutableListOf<String>()
        if (result.postedAdded > 0) {
            parts.add("added ${n(result.postedAdded, "transaction")}")
        }
        if (result.pendingPromoted > 0) {
            parts.add("${n(result.pendingPromoted, "pending hold")} settled")
        }
        if (result.pendingAdded > 0) {
            parts.add(
                if (result.postedAdded == 0) {
                    "added ${n(result.pendingAdded, "new pending hold")}"
                } else {
                    n(result.pendingAdded, "new pending hold")
                }
            )
        }
        if (result.pendingRemoved > 0) {
            parts.add("${n(result.pendingRemoved, "pending hold")} dropped")
        }

        val stillOpen = result.pendingUpdated
        if (parts.isEmpty()) {
            return if (stillOpen > 0) {
                "$accountName: up to date. ${n(stillOpen, "pending hold")} still open."
            } else {
                "$accountName: up to date."
            }
        }
        if (stillOpen > 0 && result.pendingAdded == 0) {
            parts.add("${n(stillOpen, "pending hold")} still open")
        }
        return "$accountName: ${parts.joinToString(". ")}."
    }

    fun overall(results: List<AccountSyncResult>): String {
        if (results.isEmpty()) return "up to date"
        val errors = results.count { it.error != null }
        val added = results.sumOf { it.postedAdded + it.pendingAdded }
        val promoted = results.sumOf { it.pendingPromoted }
        return when {
            errors > 0 && added == 0 -> "finished with ${n(errors, "error")}"
            errors > 0 -> "imported ${n(added, "new transaction")}; ${n(errors, "error")}"
            added > 0 && promoted > 0 ->
                "imported ${n(added, "new transaction")}. ${n(promoted, "pending hold")} settled"
            added > 0 -> "imported ${n(added, "new transaction")}"
            promoted > 0 -> "${n(promoted, "pending hold")} settled"
            else -> "up to date"
        }
    }

    internal fun n(count: Int, singular: String): String {
        val plural = if (singular.endsWith("s")) singular else "${singular}s"
        return if (count == 1) "1 $singular" else "$count $plural"
    }
}
