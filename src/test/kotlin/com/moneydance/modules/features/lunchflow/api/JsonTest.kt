package com.moneydance.modules.features.lunchflow.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonTest {
    @Test
    fun parsesObjectStringAndNumber() {
        val root = parseJson("""{"a":"x","n":12}""").requireObj("root")
        assertEquals("x", root.str("a"))
        assertEquals(12L, root.long("n"))
    }

    @Test
    fun unicodeEscape() {
        val root = parseJson("""{"a":"\u0041"}""").requireObj("root")
        assertEquals("A", root.str("a"))
    }

    @Test
    fun badUnicodeEscapeIsParseError() {
        val err = assertFailsWith<LunchFlowException.Parse> {
            parseJson("""{"a":"\uZZZZ"}""")
        }
        assertTrue(err.message!!.contains("unicode"))
    }
}
