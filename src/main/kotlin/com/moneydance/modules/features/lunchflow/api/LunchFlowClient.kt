package com.moneydance.modules.features.lunchflow.api

import com.moneydance.modules.features.lunchflow.Version

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class LunchFlowClient(
    private val apiKey: String,
    private val http: HttpGet = JdkHttpGet(),
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    fun listAccounts(): List<LunchFlowAccount> {
        val key = apiKey.trim()
        if (key.isEmpty()) throw LunchFlowException.MissingKey()
        val response = get("/accounts")
        return when (response.status) {
            200 -> AccountsParser.parse(response.body)
            else -> throw mapStatus(response.status)
        }
    }

    fun getTransactions(
        accountId: Long,
        from: String? = null,
        to: String? = null,
        includePending: Boolean = true
    ): List<LunchFlowTransaction> {
        val key = apiKey.trim()
        if (key.isEmpty()) throw LunchFlowException.MissingKey()
        val q = StringBuilder("/accounts/").append(accountId).append("/transactions")
            .append("?include_pending=").append(includePending)
        if (!from.isNullOrBlank()) q.append("&from=").append(from.trim())
        if (!to.isNullOrBlank()) q.append("&to=").append(to.trim())
        val response = get(q.toString())
        return when (response.status) {
            200 -> TransactionsParser.parse(response.body)
            else -> throw mapStatus(response.status)
        }
    }

    private fun get(path: String): RawHttpResponse {
        val url = baseUrl.trimEnd('/') + path
        return try {
            http.get(
                url,
                mapOf(
                    "x-api-key" to apiKey.trim(),
                    "Accept" to "application/json",
                    "User-Agent" to Version.USER_AGENT
                )
            )
        } catch (e: LunchFlowException) {
            throw e
        } catch (e: Exception) {
            throw LunchFlowException.Network(e)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://www.lunchflow.app/api/v1"

        fun mapStatus(status: Int): LunchFlowException = when (status) {
            401 -> LunchFlowException.Unauthorized()
            403 -> LunchFlowException.Forbidden()
            404 -> LunchFlowException.NotFound()
            else -> LunchFlowException.Http(status)
        }
    }
}

class JdkHttpGet(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : HttpGet {
    override fun get(url: String, headers: Map<String, String>): RawHttpResponse {
        val builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
        headers.forEach { (name, value) -> builder.header(name, value) }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return RawHttpResponse(response.statusCode(), response.body() ?: "")
    }
}
