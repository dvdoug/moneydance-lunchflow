package com.moneydance.modules.features.lunchflow.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LunchFlowClientTest {
    @Test
    fun listAccountsSuccess() {
        val json = javaClass.getResource("/accounts.json")!!.readText()
        val client = LunchFlowClient("secret-key-value", http = { _, _ -> RawHttpResponse(200, json) })
        val accounts = client.listAccounts()
        assertEquals(2, accounts.size)
        assertEquals("Everyday", accounts[0].name)
    }

    @Test
    fun unauthorized() {
        val client = LunchFlowClient("secret-key-value", http = { _, _ -> RawHttpResponse(401, """{"error":"Unauthorized"}""") })
        val err = assertFailsWith<LunchFlowException.Unauthorized> { client.listAccounts() }
        assertFalse(err.message!!.contains("secret-key-value"))
        assertTrue(err.message!!.contains("third-party"))
    }

    @Test
    fun forbidden() {
        val client = LunchFlowClient("secret-key-value", http = { _, _ -> RawHttpResponse(403, """{"error":"Forbidden"}""") })
        val err = assertFailsWith<LunchFlowException.Forbidden> { client.listAccounts() }
        assertFalse(err.message!!.contains("secret-key-value"))
    }

    @Test
    fun networkFailure() {
        val client = LunchFlowClient(
            "secret-key-value",
            http = { _, _ -> throw java.io.IOException("connection reset") }
        )
        val err = assertFailsWith<LunchFlowException.Network> { client.listAccounts() }
        assertFalse(err.message!!.contains("secret-key-value"))
    }

    @Test
    fun sendsApiKeyHeaderNotInUrl() {
        var capturedUrl = ""
        var capturedHeaders = emptyMap<String, String>()
        val json = """{"accounts":[],"total":0}"""
        val client = LunchFlowClient(
            "secret-key-value",
            http = { url, headers ->
                capturedUrl = url
                capturedHeaders = headers
                RawHttpResponse(200, json)
            }
        )
        client.listAccounts()
        assertEquals("https://www.lunchflow.app/api/v1/accounts", capturedUrl)
        assertEquals("secret-key-value", capturedHeaders["x-api-key"])
        assertFalse(capturedUrl.contains("secret-key-value"))
    }

    @Test
    fun getTransactionsBuildsQuery() {
        var capturedUrl = ""
        val json = javaClass.getResource("/transactions.json")!!.readText()
        val client = LunchFlowClient(
            "secret-key-value",
            http = { url, _ ->
                capturedUrl = url
                RawHttpResponse(200, json)
            }
        )
        val txns = client.getTransactions(42, from = "2026-03-01", to = "2026-03-31")
        assertEquals(2, txns.size)
        assertEquals(
            "https://www.lunchflow.app/api/v1/accounts/42/transactions?include_pending=true&from=2026-03-01&to=2026-03-31",
            capturedUrl
        )
    }

    @Test
    fun blankKeyDoesNotCallHttp() {
        var called = false
        val client = LunchFlowClient("  ", http = { _, _ ->
            called = true
            RawHttpResponse(200, "{}")
        })
        assertFailsWith<LunchFlowException.MissingKey> { client.listAccounts() }
        assertFalse(called)
    }
}
