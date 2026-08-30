package com.moneydance.modules.features.lunchflow.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountMappingCodecTest {
    @Test
    fun roundTrip() {
        val original = listOf(
            AccountMapping(42, "uuid-1", "2026-03-01", "2026-03-10"),
            AccountMapping(99, "uuid-2", null, null)
        )
        val parsed = AccountMappingCodec.fromJson(AccountMappingCodec.toJson(original))
        assertEquals(2, parsed.size)
        assertEquals(42L, parsed[0].lunchFlowAccountId)
        assertEquals("uuid-1", parsed[0].moneydanceAccountUuid)
        assertEquals("2026-03-01", parsed[0].syncStartDate)
        assertEquals("2026-03-10", parsed[0].lastPostedDate)
        assertEquals(99L, parsed[1].lunchFlowAccountId)
        assertNull(parsed[1].syncStartDate)
    }

    @Test
    fun roundTripNames() {
        val original = listOf(
            AccountMapping(42, "uuid-1", "2026-03-01", null, "Current", "Lloyds")
        )
        val parsed = AccountMappingCodec.fromJson(AccountMappingCodec.toJson(original)).single()
        assertEquals("Current", parsed.lunchFlowName)
        assertEquals("Lloyds", parsed.institutionName)
    }

    @Test
    fun emptyJson() {
        assertEquals(emptyList(), AccountMappingCodec.fromJson(null))
        assertEquals(emptyList(), AccountMappingCodec.fromJson(""))
    }
}
