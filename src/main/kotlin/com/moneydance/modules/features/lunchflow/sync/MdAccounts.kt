package com.moneydance.modules.features.lunchflow.sync

import com.infinitekind.moneydance.model.Account
import com.infinitekind.moneydance.model.AccountBook
import com.infinitekind.moneydance.model.AccountUtil
import com.infinitekind.moneydance.model.AcctFilter

object MdAccounts {
    private val MAPPABLE = setOf(
        Account.AccountType.BANK,
        Account.AccountType.CREDIT_CARD,
        Account.AccountType.ASSET,
        Account.AccountType.LIABILITY
    )

    fun listMappable(book: AccountBook): List<Account> {
        val filter = object : AcctFilter() {
            override fun matches(acct: Account?): Boolean {
                if (acct == null) return false
                return MdAccess.accountType(acct) in MAPPABLE && !MdAccess.isInactive(acct)
            }

            override fun format(acct: Account): String? = MdAccess.fullAccountName(acct)
        }
        return AccountUtil.allMatchesForSearch(book, filter)
            .sortedBy { MdAccess.fullAccountName(it).lowercase() }
    }
}
