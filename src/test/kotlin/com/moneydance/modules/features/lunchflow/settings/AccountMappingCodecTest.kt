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

    @Test
    fun omittedFromIsNullNotThisMonth() {
        val mapping = AccountMapping(1, "uuid")
        assertNull(mapping.syncStartDate)
    }

    @Test
    fun newRowGetsDefaultFromExistingKeepsBlank() {
        val existing = AccountMapping(1, "uuid", null, "2026-08-01")
        assertNull(AccountMapping.fromDateForRow(existing))
        assertEquals("2026-08-01", AccountMapping.fromDateForRow(existing.copy(syncStartDate = "2026-08-01")))
        val fresh = AccountMapping.fromDateForRow(null)
        assertEquals(AccountMapping.defaultStartDate(), fresh)
    }

    @Test
    fun keepUnlistedPreservesSavedWhenRefreshOmitsThem() {
        val table = listOf(AccountMapping(1, "a", "2026-08-01"))
        val saved = listOf(
            AccountMapping(1, "a", "2026-01-01", "2026-08-20"),
            AccountMapping(2, "b", "2026-03-01", "2026-08-15")
        )
        val merged = AccountMapping.keepUnlisted(table, saved, setOf(1L))
        assertEquals(2, merged.size)
        assertEquals(1L, merged[0].lunchFlowAccountId)
        assertEquals(2L, merged[1].lunchFlowAccountId)
        assertEquals("2026-08-15", merged[1].lastPostedDate)
    }

    @Test
    fun keepUnlistedDropsUnmappedVisibleRow() {
        val table = emptyList<AccountMapping>()
        val saved = listOf(AccountMapping(1, "a", "2026-08-01"))
        val merged = AccountMapping.keepUnlisted(table, saved, setOf(1L))
        assertEquals(emptyList(), merged)
    }
}
