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
    val syncStartDate: String? = defaultStartDate(),
    val lastPostedDate: String? = null,
    val lunchFlowName: String? = null,
    val institutionName: String? = null
) {
    fun afterSuccessfulImport(latestPosted: String?): AccountMapping {
        val latest = latestPosted?.takeIf { it.isNotBlank() } ?: lastPostedDate
        val rolled = nextStartAfter(latest)
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

        fun plusDays(isoDate: String, days: Long): String =
            LocalDate.parse(isoDate).plusDays(days).toString()

        fun nextStartAfter(lastPosted: String?): String? {
            val last = lastPosted?.takeIf { it.isNotBlank() } ?: return null
            return plusDays(last, -OVERLAP_DAYS)
        }

        fun maxIso(a: String, b: String): String {
            val left = LocalDate.parse(a.take(10))
            val right = LocalDate.parse(b.take(10))
            return if (left >= right) a.take(10) else b.take(10)
        }

        /** Credit-card auths can sit for ~30 days; +1 for timezone/settlement lag. */
        const val OVERLAP_DAYS: Long = 31
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
