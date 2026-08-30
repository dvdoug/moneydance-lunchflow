package com.moneydance.modules.features.lunchflow.sync

import com.infinitekind.moneydance.model.Account
import com.infinitekind.moneydance.model.AccountBook

object MdAccounts {
    private val MAPPABLE = setOf(
        Account.AccountType.BANK,
        Account.AccountType.CREDIT_CARD,
        Account.AccountType.ASSET,
        Account.AccountType.LIABILITY,
        Account.AccountType.LOAN,
        Account.AccountType.INVESTMENT
    )

    fun listMappable(book: AccountBook): List<Account> {
        val out = mutableListOf<Account>()
        walk(MdAccess.rootAccount(book), out)
        return out.sortedBy { MdAccess.fullAccountName(it).lowercase() }
    }

    private fun walk(account: Account?, out: MutableList<Account>) {
        if (account == null) return
        if (MdAccess.accountType(account) in MAPPABLE && !MdAccess.isInactive(account)) {
            out.add(account)
        }
        for (i in 0 until MdAccess.subAccountCount(account)) {
            walk(account.getSubAccount(i), out)
        }
    }
}
