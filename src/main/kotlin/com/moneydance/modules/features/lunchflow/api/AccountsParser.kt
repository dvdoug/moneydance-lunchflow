package com.moneydance.modules.features.lunchflow.api

internal object AccountsParser {
    fun parse(body: String): List<LunchFlowAccount> {
        val root = parseJson(body).obj()
        val rows = root["accounts"]?.arr() ?: emptyList()
        return rows.mapIndexed { index, row ->
            val o = row.obj()
            val id = o.long("id")
                ?: throw LunchFlowException.Parse("account[$index] missing id")
            LunchFlowAccount(
                id = id,
                connectionId = o.long("connection_id"),
                name = o.str("name") ?: "Account $id",
                institutionName = o.str("institution_name") ?: "",
                provider = o.str("provider"),
                currency = o.str("currency"),
                status = o.str("status")
            )
        }
    }
}
