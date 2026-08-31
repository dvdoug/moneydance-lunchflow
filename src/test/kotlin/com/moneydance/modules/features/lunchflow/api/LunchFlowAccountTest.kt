package com.moneydance.modules.features.lunchflow.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LunchFlowAccountTest {
    @Test
    fun activeWhenBlankOrActive() {
        assertTrue(acct(null).isActive)
        assertTrue(acct("").isActive)
        assertTrue(acct("ACTIVE").isActive)
        assertTrue(acct("active").isActive)
        assertFalse(acct("EXPIRED").isActive)
        assertFalse(acct("INACTIVE").isActive)
    }

    private fun acct(status: String?) = LunchFlowAccount(
        id = 1,
        connectionId = 1,
        name = "Current",
        institutionName = "Bank",
        provider = null,
        currency = "GBP",
        status = status
    )
}
