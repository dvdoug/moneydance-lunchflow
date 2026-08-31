package com.moneydance.modules.features.lunchflow.sync

import com.infinitekind.moneydance.model.AccountBook
import com.moneydance.apps.md.view.gui.MoneydanceGUI
import com.moneydance.modules.features.lunchflow.api.LunchFlowAccount
import com.moneydance.modules.features.lunchflow.api.LunchFlowClient
import com.moneydance.modules.features.lunchflow.api.LunchFlowException
import com.moneydance.modules.features.lunchflow.api.LunchFlowTransaction
import com.moneydance.modules.features.lunchflow.settings.AccountMapping
import com.moneydance.modules.features.lunchflow.settings.SettingsStore
import com.moneydance.modules.features.lunchflow.ui.ImportStatus
import com.moneydance.modules.features.lunchflow.ui.MdNotify
import java.time.LocalDate
import javax.swing.SwingWorker

object SyncService {
    @Volatile
    var inFlight: Boolean = false
        private set

    fun start(
        book: AccountBook,
        settings: SettingsStore,
        gui: MoneydanceGUI,
        mappings: List<AccountMapping>,
        key: String,
        reason: String,
        onStatus: (String) -> Unit = {},
        onBusy: (Boolean) -> Unit = {},
        onAccounts: (List<LunchFlowAccount>) -> Unit = {},
        onMappings: (List<AccountMapping>) -> Unit = {}
    ): Boolean {
        if (inFlight) {
            MdNotify.log("skip $reason (already running)")
            return false
        }
        val mapped = mappings.filter { it.moneydanceAccountUuid.isNotBlank() }
        if (mapped.isEmpty()) {
            MdNotify.log("skip $reason (no mapped accounts)")
            onStatus("Choose a Moneydance account for at least one row.")
            return false
        }
        inFlight = true
        onBusy(true)
        val n = mapped.size
        MdNotify.log("$reason started ($n mapped account${if (n == 1) "" else "s"})")
        val startText = if (reason == "auto-import") "importing on file open" else "importing"
        MdNotify.bar(gui, startText, 0.02)
        onStatus(startText.replaceFirstChar { it.uppercase() } + "…")

        object : SwingWorker<FetchBundle, Progress>() {
            override fun doInBackground(): FetchBundle {
                val client = LunchFlowClient(key)
                val accounts = client.listAccounts()
                val byId = accounts.associateBy { it.id }
                val today = LocalDate.now().toString()
                val fetched = mapped.mapIndexed { index, mapping ->
                    val lf = byId[mapping.lunchFlowAccountId]
                    val label = lf?.name ?: "account ${mapping.lunchFlowAccountId}"
                    publish(Progress("fetching $label", (index + 0.35) / n))
                    if (lf == null) {
                        Fetched(mapping, null, null, "${mapping.lunchFlowAccountId} is not enabled for this API key.")
                    } else if (!lf.isActive) {
                        Fetched(
                            mapping,
                            lf,
                            null,
                            "This bank is not Active in Lunch Flow. Renew the connection there, then Import again."
                        )
                    } else {
                        try {
                            val oldestPending = SyncEngine.oldestOpenPendingDate(book, mapping)
                            val from = SyncEngine.fetchFromDate(mapping, oldestPending)
                            val txns = client.getTransactions(lf.id, from = from, to = today)
                            Fetched(mapping, lf, txns, null)
                        } catch (e: Exception) {
                            val msg = if (e is LunchFlowException) e.message else e.message
                            Fetched(mapping, lf, null, msg ?: "Import failed.")
                        }
                    }
                }
                return FetchBundle(accounts, fetched)
            }

            override fun process(chunks: List<Progress>) {
                val last = chunks.last()
                MdNotify.bar(gui, last.text, last.progress.coerceIn(0.02, 0.9))
                onStatus(last.text.replaceFirstChar { it.uppercase() } + "…")
            }

            override fun done() {
                try {
                    val bundle = get()
                    applyFetched(book, settings, gui, bundle, reason, onStatus, onAccounts, onMappings)
                } catch (e: Exception) {
                    val cause = e.cause ?: e
                    val msg = cause.message ?: "Import failed."
                    MdNotify.log("$reason failed: ${cause.javaClass.simpleName}: $msg", cause)
                    MdNotify.bar(gui, msg, 0.0)
                    onStatus(msg)
                } finally {
                    inFlight = false
                    onBusy(false)
                }
            }
        }.execute()
        return true
    }

    private fun applyFetched(
        book: AccountBook,
        settings: SettingsStore,
        gui: MoneydanceGUI,
        bundle: FetchBundle,
        reason: String,
        onStatus: (String) -> Unit,
        onAccounts: (List<LunchFlowAccount>) -> Unit,
        onMappings: (List<AccountMapping>) -> Unit
    ) {
        val engine = SyncEngine(book) { account -> gui.showDownloadedTxns(account) }
        val updated = mutableListOf<AccountMapping>()
        val results = mutableListOf<AccountSyncResult>()
        val lines = mutableListOf<String>()
        val total = bundle.fetched.size.coerceAtLeast(1)
        bundle.fetched.forEachIndexed { index, item ->
            if (item.error != null || item.lf == null || item.txns == null) {
                val line = item.error ?: "Error"
                updated.add(item.mapping.withLunchFlow(item.lf))
                results.add(AccountSyncResult(error = line))
                lines.add(if (item.lf != null) "${item.lf.name}: $line" else line)
                MdNotify.log("$reason ${item.lf?.name ?: item.mapping.lunchFlowAccountId}: $line")
                return@forEachIndexed
            }
            MdNotify.bar(gui, "importing ${item.lf.name}", 0.55 + 0.4 * (index + 1) / total)
            val result = engine.apply(item.mapping, item.lf, item.txns)
            val named = item.mapping.withLunchFlow(item.lf)
            updated.add(
                if (result.error == null) {
                    named.afterSuccessfulImport(result.lastPostedDate, result.oldestPendingDate)
                } else {
                    named
                }
            )
            results.add(result)
            val line = ImportStatus.line(item.lf.name, result)
            lines.add(line)
            MdNotify.log(line)
        }
        settings.setMappings(updated)
        onAccounts(bundle.accounts)
        onMappings(updated)
        val overall = ImportStatus.overall(results)
        val prefix = if (reason == "auto-import") "auto-import " else ""
        MdNotify.log("$reason finished: $overall")
        MdNotify.bar(gui, prefix + overall, 1.0)
        onStatus(lines.joinToString("\n"))
    }

    private data class Progress(val text: String, val progress: Double)
    private data class Fetched(
        val mapping: AccountMapping,
        val lf: LunchFlowAccount?,
        val txns: List<LunchFlowTransaction>?,
        val error: String?
    )
    private data class FetchBundle(
        val accounts: List<LunchFlowAccount>,
        val fetched: List<Fetched>
    )
}
