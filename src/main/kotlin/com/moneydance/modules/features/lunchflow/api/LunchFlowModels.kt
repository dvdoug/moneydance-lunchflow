package com.moneydance.modules.features.lunchflow.api

data class LunchFlowTransaction(
    val id: String?,
    val accountId: Long,
    val amount: Double,
    val currency: String?,
    val date: String,
    val merchant: String?,
    val description: String?,
    val isPending: Boolean
) {
    fun payee(): String {
        val m = merchant?.trim().orEmpty()
        if (m.isNotEmpty()) return m
        val d = description?.trim().orEmpty()
        if (d.isNotEmpty()) return d
        return "Lunch Flow"
    }

    fun memo(): String {
        val m = merchant?.trim().orEmpty()
        val d = description?.trim().orEmpty()
        return if (m.isNotEmpty() && d.isNotEmpty() && !d.equals(m, ignoreCase = true)) d else ""
    }
}

data class LunchFlowAccount(
    val id: Long,
    val connectionId: Long?,
    val name: String,
    val institutionName: String,
    val provider: String?,
    val currency: String?,
    val status: String?
) {
    val isActive: Boolean get() = status.isNullOrBlank() || status.equals("ACTIVE", ignoreCase = true)
}

data class RawHttpResponse(
    val status: Int,
    val body: String
)

fun interface HttpGet {
    fun get(url: String, headers: Map<String, String>): RawHttpResponse
}
