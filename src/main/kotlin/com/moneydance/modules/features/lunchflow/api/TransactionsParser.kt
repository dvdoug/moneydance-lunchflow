package com.moneydance.modules.features.lunchflow.api

internal object TransactionsParser {
    fun parse(body: String): List<LunchFlowTransaction> {
        val root = parseJson(body).requireObj("transactions response")
        val rows = root.requireArr("transactions")
        return rows.mapIndexed { index, row ->
            val o = row.obj()
            val accountId = o.long("accountId")
                ?: throw LunchFlowException.Parse("transaction[$index] missing accountId")
            val date = o.str("date")
                ?: throw LunchFlowException.Parse("transaction[$index] missing date")
            val amount = o.double("amount")
                ?: throw LunchFlowException.Parse("transaction[$index] missing amount")
            val idVal = o["id"]
            val id = when (idVal) {
                null, JsonVal.Null -> null
                else -> idVal.str()?.takeIf { it.isNotBlank() }
            }
            LunchFlowTransaction(
                id = id,
                accountId = accountId,
                amount = amount,
                currency = o.str("currency"),
                date = date,
                merchant = o.str("merchant"),
                description = o.str("description"),
                isPending = o.bool("isPending") ?: false
            )
        }
    }
}
