package com.moneydance.modules.features.lunchflow.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AccountsParserTest {
    @Test
    fun parsesFixtureAccounts() {
        val json = javaClass.getResource("/accounts.json")!!.readText()
        val accounts = AccountsParser.parse(json)
        assertEquals(2, accounts.size)
        assertEquals(42L, accounts[0].id)
        assertEquals("Everyday", accounts[0].name)
        assertEquals("Example Bank", accounts[0].institutionName)
        assertEquals("GBP", accounts[0].currency)
        assertEquals("ACTIVE", accounts[0].status)
        assertEquals(99L, accounts[1].id)
        assertEquals("gocardless", accounts[1].provider)
    }

    @Test
    fun emptyAccountsList() {
        val accounts = AccountsParser.parse("""{"accounts":[],"total":0}""")
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun missingIdFails() {
        assertFailsWith<LunchFlowException.Parse> {
            AccountsParser.parse("""{"accounts":[{"name":"No id"}]}""")
        }
    }

    @Test
    fun errorEnvelopeIsParseError() {
        assertFailsWith<LunchFlowException.Parse> {
            AccountsParser.parse("""{"error":"Unauthorized"}""")
        }
    }

    @Test
    fun emptyObjectIsParseError() {
        assertFailsWith<LunchFlowException.Parse> {
            AccountsParser.parse("{}")
        }
    }
}
