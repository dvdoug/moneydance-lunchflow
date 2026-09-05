package com.moneydance.modules.features.lunchflow.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionsParserTest {
    @Test
    fun parsesPostedAndPending() {
        val json = javaClass.getResource("/transactions.json")!!.readText()
        val txns = TransactionsParser.parse(json)
        assertEquals(2, txns.size)
        assertEquals("p1", txns[0].id)
        assertEquals(-12.5, txns[0].amount)
        assertFalse(txns[0].isPending)
        assertEquals("Coffee", txns[0].payee())
        assertEquals("Latte", txns[0].memo())
        assertNull(txns[1].id)
        assertTrue(txns[1].isPending)
    }

    @Test
    fun numericIdBecomesString() {
        val txns = TransactionsParser.parse(
            """{"transactions":[{"id":123,"accountId":1,"amount":-1,"date":"2026-03-01","isPending":false}]}"""
        )
        assertEquals("123", txns.single().id)
    }

    @Test
    fun errorEnvelopeIsParseError() {
        assertFailsWith<LunchFlowException.Parse> {
            TransactionsParser.parse("""{"error":"nope"}""")
        }
    }

    @Test
    fun emptyObjectIsParseError() {
        assertFailsWith<LunchFlowException.Parse> {
            TransactionsParser.parse("{}")
        }
    }
}
