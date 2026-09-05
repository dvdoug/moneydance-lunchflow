package com.moneydance.modules.features.lunchflow.settings

import com.moneydance.modules.features.lunchflow.api.long as jsonLong
import com.moneydance.modules.features.lunchflow.api.parseJson
import com.moneydance.modules.features.lunchflow.api.str as jsonStr
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class AccountMapping(
    val lunchFlowAccountId: Long,
    val moneydanceAccountUuid: String,
    val syncStartDate: String? = null,
    val lastPostedDate: String? = null,
    val lunchFlowName: String? = null,
    val institutionName: String? = null
) {
    fun afterSuccessfulImport(latestPosted: String?, oldestPending: String? = null): AccountMapping {
        val latest = latestPosted?.takeIf { it.isNotBlank() } ?: lastPostedDate
        val rolled = lookbackFloor(latest, oldestPending)
        val nextStart = when {
            rolled == null -> syncStartDate
            syncStartDate.isNullOrBlank() -> rolled
            else -> maxIso(syncStartDate, rolled)
        }
        return copy(
            lastPostedDate = latest,
            syncStartDate = nextStart
        )
    }

    fun withLunchFlow(account: com.moneydance.modules.features.lunchflow.api.LunchFlowAccount?): AccountMapping {
        if (account == null) return this
        return copy(
            lunchFlowName = account.name,
            institutionName = account.institutionName.takeIf { it.isNotBlank() }
        )
    }

    companion object {
        fun defaultStartDate(): String =
            YearMonth.now(ZoneId.systemDefault()).atDay(1).toString()

        fun fromDateForRow(existing: AccountMapping?): String? =
            if (existing == null) defaultStartDate() else existing.syncStartDate

        fun keepUnlisted(fromTable: List<AccountMapping>, saved: List<AccountMapping>, visibleIds: Set<Long>): List<AccountMapping> {
            val kept = saved.filter { it.lunchFlowAccountId !in visibleIds && it.moneydanceAccountUuid.isNotBlank() }
            return fromTable + kept
        }

        fun plusDays(isoDate: String, days: Long): String =
            LocalDate.parse(isoDate).plusDays(days).toString()

        fun lookbackFloor(lastPosted: String?, oldestPending: String?): String? {
            val posted = lastPosted?.takeIf { it.isNotBlank() }?.let { plusDays(it, -POSTED_OVERLAP_DAYS) }
            val pending = oldestPending?.takeIf { it.isNotBlank() }?.let { plusDays(it, -PENDING_PAD_DAYS) }
            return minIsoOrNull(posted, pending)
        }

        fun fetchFromDate(syncStart: String?, lastPosted: String?, oldestPending: String?): String? {
            val persisted = syncStart?.takeIf { it.isNotBlank() }
            val computed = lookbackFloor(lastPosted, oldestPending)
            return minIsoOrNull(persisted, computed) ?: persisted ?: computed
        }

        fun maxIso(a: String, b: String): String {
            val left = LocalDate.parse(a.take(10))
            val right = LocalDate.parse(b.take(10))
            return if (left >= right) a.take(10) else b.take(10)
        }

        fun minIsoOrNull(a: String?, b: String?): String? {
            val left = a?.takeIf { it.isNotBlank() }?.take(10)
            val right = b?.takeIf { it.isNotBlank() }?.take(10)
            if (left == null) return right
            if (right == null) return left
            return if (LocalDate.parse(left) <= LocalDate.parse(right)) left else right
        }

        /** Late posted rows (timezone, weekend, holiday clearing), not auth life. */
        const val POSTED_OVERLAP_DAYS: Long = 7
        const val PENDING_PAD_DAYS: Long = 1
    }
}

object AccountMappingCodec {
    fun toJson(mappings: List<AccountMapping>): String {
        val rows = mappings.joinToString(",") { m ->
            buildString {
                append("{")
                append("\"lf\":").append(m.lunchFlowAccountId)
                append(",\"md\":").append(jsonString(m.moneydanceAccountUuid))
                if (!m.syncStartDate.isNullOrBlank()) {
                    append(",\"from\":").append(jsonString(m.syncStartDate))
                }
                if (!m.lastPostedDate.isNullOrBlank()) {
                    append(",\"last\":").append(jsonString(m.lastPostedDate))
                }
                if (!m.lunchFlowName.isNullOrBlank()) {
                    append(",\"name\":").append(jsonString(m.lunchFlowName))
                }
                if (!m.institutionName.isNullOrBlank()) {
                    append(",\"inst\":").append(jsonString(m.institutionName))
                }
                append("}")
            }
        }
        return "{\"mappings\":[$rows]}"
    }

    fun fromJson(text: String?): List<AccountMapping> {
        if (text.isNullOrBlank()) return emptyList()
        val root = parseJson(text).obj()
        return root["mappings"]?.arr().orEmpty().mapNotNull { row ->
            val o = row.obj()
            val lf = o.jsonLong("lf") ?: return@mapNotNull null
            val md = o.jsonStr("md")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            AccountMapping(
                lunchFlowAccountId = lf,
                moneydanceAccountUuid = md,
                syncStartDate = o.jsonStr("from")?.takeIf { it.isNotBlank() },
                lastPostedDate = o.jsonStr("last")?.takeIf { it.isNotBlank() },
                lunchFlowName = o.jsonStr("name")?.takeIf { it.isNotBlank() },
                institutionName = o.jsonStr("inst")?.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        for (c in value) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
        append('"')
    }
}
