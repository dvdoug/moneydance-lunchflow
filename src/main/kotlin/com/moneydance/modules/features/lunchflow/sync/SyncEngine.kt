package com.moneydance.modules.features.lunchflow.sync

import com.infinitekind.moneydance.model.Account
import com.infinitekind.moneydance.model.AccountBook
import com.infinitekind.moneydance.model.ParentTxn
import com.moneydance.modules.features.lunchflow.api.LunchFlowAccount
import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import com.moneydance.modules.features.lunchflow.settings.AccountMapping
import java.time.LocalDate

data class AccountSyncResult(
    val postedAdded: Int = 0,
    val postedSkipped: Int = 0,
    val pendingAdded: Int = 0,
    val pendingUpdated: Int = 0,
    val pendingRemoved: Int = 0,
    val pendingPromoted: Int = 0,
    val pendingPayees: List<String> = emptyList(),
    val lastPostedDate: String? = null,
    val error: String? = null
)

class SyncEngine(
    private val book: AccountBook,
    private val processDownloaded: (Account) -> Unit = {}
) {

    fun apply(
        mapping: AccountMapping,
        lfAccount: LunchFlowAccount,
        txns: List<LunchFlowTransaction>
    ): AccountSyncResult {
        val mdAccount = book.getAccountByUUID(mapping.moneydanceAccountUuid)
            ?: return AccountSyncResult(error = "Mapped Moneydance account is missing.")
        val mdCurrency = MdAccess.currencyId(mdAccount)
        val lfCurrency = lfAccount.currency?.trim().orEmpty()
        if (lfCurrency.isNotEmpty() && !lfCurrency.equals(mdCurrency, ignoreCase = true)) {
            return AccountSyncResult(
                error = "Currency mismatch: Lunch Flow ${lfAccount.currency} vs Moneydance $mdCurrency."
            )
        }

        val known = collectRegisterFitIds(mdAccount)
        pruneStaleDownloads(mdAccount, known)
        stripPendingFromDownloads(mdAccount)
        val pendingLf = txns.filter { it.isPending }
        val postedLf = txns.filter { !it.isPending && !it.id.isNullOrBlank() }

        val pendingRegister = mutableMapOf<String, ParentTxn>()
        for (txn in MdAccess.txnsForAccount(book, mdAccount)) {
            if (txn !is ParentTxn) continue
            if (!MdAccess.sameAccount(MdAccess.accountOf(txn), mdAccount)) continue
            val id = MdAccess.registerFiTxnId(txn, FitIds.PROTOCOL)
            if (FitIds.isOurs(id) || FitIds.isPending(id)) {
                sanitizeOrigPayee(txn)
            }
            if (!MdAccess.isNew(txn)) continue
            if (FitIds.isPending(id) && id != null) pendingRegister[id] = txn
        }

        val desiredPending = linkedMapOf<String, LunchFlowTransaction>()
        for (txn in pendingLf) {
            desiredPending[FitIds.pendingKey(lfAccount.id, txn)] = txn
        }

        val newPosted = postedLf.filter { FitIds.posted(lfAccount.id, it.id!!) !in known }
        val dropped = pendingRegister.keys.filter { it !in desiredPending }.mapNotNull { key ->
            val snap = snapshotFromParent(mdAccount, pendingRegister[key]!!) ?: return@mapNotNull null
            key to snap
        }
        val promotions = PendingMatch.uniquePairs(dropped, newPosted)
        val promotedPostedIds = promotions.mapNotNull { it.posted.id }.toSet()
        val promotedPendingKeys = promotions.map { it.pendingKey }.toSet()

        var postedAdded = 0
        var postedSkipped = 0
        var pendingAdded = 0
        var pendingUpdated = 0
        var pendingRemoved = 0
        var pendingPromoted = 0
        var latestPosted: String? = mapping.lastPostedDate

        for (pair in promotions) {
            val existing = pendingRegister.remove(pair.pendingKey) ?: continue
            val fitId = FitIds.posted(lfAccount.id, pair.posted.id!!)
            MdAccess.setDescription(existing, pair.posted.payee())
            sanitizeOrigPayee(existing)
            MdAccess.clearPendingFlag(existing)
            MdAccess.setRegisterFitId(existing, fitId)
            known.add(fitId)
            pendingPromoted++
            latestPosted = maxDate(latestPosted, pair.posted.date)
        }

        for (txn in postedLf) {
            val fitId = FitIds.posted(lfAccount.id, txn.id!!)
            if (txn.id in promotedPostedIds) continue
            if (fitId in known) {
                postedSkipped++
                latestPosted = maxDate(latestPosted, txn.date)
                continue
            }
            addDownloadTxn(mdAccount, txn, fitId, pending = false)
            known.add(fitId)
            postedAdded++
            latestPosted = maxDate(latestPosted, txn.date)
        }

        for ((key, txn) in desiredPending) {
            val existing = pendingRegister.remove(key)
            if (existing != null) {
                pendingUpdated++
            } else if (key in known) {
                pendingUpdated++
            } else {
                addDownloadTxn(mdAccount, txn, key, pending = true)
                known.add(key)
                pendingAdded++
            }
        }

        for ((key, existing) in pendingRegister) {
            if (key in promotedPendingKeys) continue
            MdAccess.deleteTxn(existing)
            pendingRemoved++
        }

        finishDownloads(mdAccount)
        for ((key, txn) in desiredPending) {
            val parent = MdAccess.findByFitId(book, mdAccount, FitIds.PROTOCOL, key) ?: continue
            labelRegisterPending(parent, txn.payee())
        }

        return AccountSyncResult(
            postedAdded = postedAdded,
            postedSkipped = postedSkipped,
            pendingAdded = pendingAdded,
            pendingUpdated = pendingUpdated,
            pendingRemoved = pendingRemoved,
            pendingPromoted = pendingPromoted,
            lastPostedDate = latestPosted
        )
    }

    private fun addDownloadTxn(
        account: Account,
        txn: LunchFlowTransaction,
        fitId: String,
        pending: Boolean
    ) {
        MdAccess.addDownload(
            account,
            isoToDateInt(txn.date),
            MdAccess.toMinorUnits(account, txn.amount),
            txn.payee(),
            txn.memo(),
            fitId,
            pending,
            MdAccess.currencyId(account)
        )
    }

    private fun finishDownloads(account: Account) {
        val downloaded = MdAccess.downloadedTxns(account) ?: return
        MdAccess.sortTxns(downloaded)
        MdAccess.syncList(downloaded)
        MdAccess.notifyDownloaded(downloaded)
        MdAccess.downloadedUpdated(account)
        processDownloaded(account)
    }

    private fun pruneStaleDownloads(account: Account, registerIds: Set<String>) {
        val downloaded = MdAccess.downloadedTxns(account) ?: return
        val toRemove = mutableListOf<com.infinitekind.moneydance.model.OnlineTxn>()
        for (i in 0 until MdAccess.txnCount(downloaded)) {
            val row = MdAccess.txnAt(downloaded, i) ?: continue
            val fitId = MdAccess.fiTxnId(row)
            if (fitId.isNullOrBlank() || fitId in registerIds || MdAccess.isAcceptedDownload(row)) {
                toRemove.add(row)
            }
        }
        if (toRemove.isEmpty()) return
        toRemove.forEach { MdAccess.removeTxn(downloaded, it) }
        MdAccess.syncList(downloaded)
        MdAccess.downloadedUpdated(account)
    }

    private fun stripPendingFromDownloads(account: Account) {
        val downloaded = MdAccess.downloadedTxns(account) ?: return
        var changed = false
        for (i in 0 until MdAccess.txnCount(downloaded)) {
            val row = MdAccess.txnAt(downloaded, i) ?: continue
            val name = MdAccess.getName(row) ?: continue
            if (!name.startsWith(FitIds.PENDING_LABEL)) continue
            val clean = FitIds.stripPendingLabel(name)
            MdAccess.setName(row, clean)
            MdAccess.setMerchantName(row, clean)
            changed = true
        }
        if (!changed) return
        MdAccess.syncList(downloaded)
    }

    private fun labelRegisterPending(txn: ParentTxn, payee: String) {
        if (!MdAccess.isNew(txn)) return
        sanitizeOrigPayee(txn)
        val current = MdAccess.getDescription(txn).orEmpty()
        if (!current.startsWith(FitIds.PENDING_LABEL)) {
            MdAccess.setDescription(txn, FitIds.withPendingLabel(current.ifBlank { payee }))
        }
        txn.setParameter(FitIds.PARAM_PENDING, true)
        txn.syncItem()
    }

    private fun sanitizeOrigPayee(txn: ParentTxn) {
        val orig = txn.getParameter(FitIds.ORIG_PAYEE_TAG, "") ?: return
        if (!orig.startsWith(FitIds.PENDING_LABEL)) return
        txn.setParameter(FitIds.ORIG_PAYEE_TAG, FitIds.stripPendingLabel(orig))
        txn.syncItem()
    }

    private fun collectRegisterFitIds(account: Account): MutableSet<String> {
        val ids = mutableSetOf<String>()
        for (txn in MdAccess.txnsForAccount(book, account)) {
            if (txn !is ParentTxn) continue
            if (!MdAccess.sameAccount(MdAccess.accountOf(txn), account)) continue
            val id = MdAccess.registerFiTxnId(txn, FitIds.PROTOCOL)
            if (!id.isNullOrBlank()) ids.add(id)
        }
        return ids
    }

    private fun snapshotFromParent(mdAccount: Account, parent: ParentTxn): LunchFlowTransaction? {
        val desc = FitIds.stripPendingLabel(MdAccess.getDescription(parent).orEmpty()).trim()
        return LunchFlowTransaction(
            id = null,
            accountId = 0,
            amount = MdAccess.toMajorUnits(mdAccount, MdAccess.getValue(parent)),
            currency = MdAccess.currencyId(mdAccount),
            date = dateIntToIso(MdAccess.getDateInt(parent)),
            merchant = desc,
            description = desc,
            isPending = true
        )
    }

    companion object {
        fun fetchFromDate(mapping: AccountMapping): String? {
            mapping.syncStartDate?.takeIf { it.isNotBlank() }?.let { return it }
            val last = mapping.lastPostedDate?.takeIf { it.isNotBlank() } ?: return null
            return AccountMapping.nextStartAfter(last)
        }

        fun isoToDateInt(iso: String): Int {
            val d = LocalDate.parse(iso.take(10))
            return d.year * 10000 + d.monthValue * 100 + d.dayOfMonth
        }

        fun dateIntToIso(dateInt: Int): String {
            val y = dateInt / 10000
            val m = (dateInt / 100) % 100
            val d = dateInt % 100
            return "%04d-%02d-%02d".format(y, m, d)
        }

        fun maxDate(a: String?, b: String?): String? {
            if (a.isNullOrBlank()) return b
            if (b.isNullOrBlank()) return a
            return if (a >= b) a else b
        }
    }
}
