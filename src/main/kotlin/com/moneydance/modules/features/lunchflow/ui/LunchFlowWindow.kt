package com.moneydance.modules.features.lunchflow.ui

import com.infinitekind.moneydance.model.AccountBook
import com.moneydance.apps.md.view.gui.MoneydanceGUI
import com.moneydance.apps.md.view.gui.SecondaryDialog
import com.moneydance.modules.features.lunchflow.Main
import com.moneydance.modules.features.lunchflow.api.LunchFlowAccount
import com.moneydance.modules.features.lunchflow.api.LunchFlowClient
import com.moneydance.modules.features.lunchflow.settings.AccountMapping
import com.moneydance.modules.features.lunchflow.settings.ApiKeyMask
import com.moneydance.modules.features.lunchflow.settings.SettingsStore
import com.moneydance.modules.features.lunchflow.sync.SyncService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.SwingWorker

class LunchFlowWindow(
    private val mdGUI: MoneydanceGUI,
    private val book: AccountBook?,
    private val settings: SettingsStore?,
    var onGoneAway: (() -> Unit)? = null,
    var onAutomaticImportChanged: ((Boolean) -> Unit)? = null
) : SecondaryDialog(mdGUI, mdGUI.getTopLevelFrame(), "Lunch Flow", false) {

    private val keyField = JPasswordField()
    private val savedLabel = JLabel(" ")
    private val statusArea = JTextArea(3, 40).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = null
        isOpaque = false
        border = BorderFactory.createEmptyBorder()
    }
    private val mappingPanel = AccountMappingPanel(book, mdGUI)
    private val refreshButton = JButton("Refresh accounts")
    private val saveButton = JButton("Save key")
    private val forgetButton = JButton("Remove key")
    private val syncButton = JButton("Import")
    private val importOnOpenBox = JCheckBox("Automatically import")
    private var busy = false
    private var lfAccounts: List<LunchFlowAccount> = emptyList()

    init {
        setUsesDataFile(true)
        setEscapeKeyCancels(true)
        setRememberSizeLocationKeys("lunchflow.sz", "lunchflow.loc", Dimension(760, 560))
        layout = BorderLayout(0, 0)

        mappingPanel.setSavedMappings(settings?.mappings().orEmpty())
        mappingPanel.border = BorderFactory.createEmptyBorder(0, 20, 8, 20)
        showSavedAccountRows()

        add(buildHeader(), BorderLayout.NORTH)
        add(mappingPanel, BorderLayout.CENTER)
        add(buildFooter(), BorderLayout.SOUTH)

        saveButton.addActionListener { saveKey() }
        refreshButton.addActionListener { refreshAccounts() }
        forgetButton.addActionListener { forgetKey() }
        syncButton.addActionListener { syncNow() }
        importOnOpenBox.isSelected = settings?.importOnOpen() ?: false
        importOnOpenBox.addActionListener {
            val on = importOnOpenBox.isSelected
            settings?.setImportOnOpen(on)
            MdNotify.log(if (on) "automatic import enabled" else "automatic import disabled")
            onAutomaticImportChanged?.invoke(on)
        }

        setBusyButtons(true)
        rootPane.defaultButton = syncButton
        if (settings == null || book == null) {
            setStatus("Open a data file to continue.")
        }
        refreshSavedLabel()
        minimumSize = Dimension(640, 480)
    }

    private fun buildHeader(): JPanel {
        val header = JPanel(GridBagLayout())
        header.border = BorderFactory.createEmptyBorder(16, 20, 8, 20)
        val gbc = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            anchor = GridBagConstraints.WEST
            insets = Insets(0, 0, 6, 0)
        }

        val title = JLabel("Lunch Flow")
        title.font = title.font.deriveFont(Font.BOLD, title.font.size2D + 3f)
        header.add(title, gbc)

        gbc.insets = Insets(0, 0, 4, 0)
        header.add(JLabel("Import bank transactions via Open Banking (UK, EU, and more)."), gbc)

        val help = JButton("Setup guide")
        help.isBorderPainted = false
        help.isContentAreaFilled = false
        help.isOpaque = false
        help.horizontalAlignment = SwingConstants.LEFT
        help.margin = Insets(0, 0, 0, 0)
        help.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        help.foreground = Color(0x0B, 0x57, 0xD0)
        help.addActionListener { mdGUI.showInternetURL(DOCS_URL) }
        gbc.insets = Insets(0, 0, 12, 0)
        header.add(help, gbc)

        gbc.insets = Insets(0, 0, 4, 0)
        header.add(JLabel("API key"), gbc)
        gbc.insets = Insets(0, 0, 2, 0)
        header.add(keyField, gbc)
        gbc.insets = Insets(0, 0, 8, 0)
        header.add(savedLabel, gbc)

        val keyButtons = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        keyButtons.add(saveButton)
        keyButtons.add(refreshButton)
        keyButtons.add(forgetButton)
        header.add(keyButtons, gbc)

        gbc.insets = Insets(4, 0, 0, 0)
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 0.0
        header.add(JScrollPane(statusArea).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(40, 54)
        }, gbc)
        return header
    }

    private fun buildFooter(): JPanel {
        val footer = JPanel(BorderLayout())
        footer.border = BorderFactory.createEmptyBorder(4, 20, 12, 20)
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        actions.add(syncButton)
        actions.add(importOnOpenBox)
        val close = JButton("Close")
        close.addActionListener { goAway() }
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        right.add(close)
        val buttons = JPanel(BorderLayout())
        buttons.add(actions, BorderLayout.WEST)
        buttons.add(right, BorderLayout.EAST)
        footer.add(buttons, BorderLayout.NORTH)
        val note = JLabel(Main.THIRD_PARTY_DISCLAIMER)
        note.font = note.font.deriveFont(note.font.size2D - 1f)
        note.foreground = Color.GRAY
        note.border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
        footer.add(note, BorderLayout.SOUTH)
        return footer
    }

    override fun getWindowName(): String = "Lunch Flow"

    fun bringToFront() {
        isVisible = true
        toFront()
        requestFocus()
        if (!settings?.apiKey().isNullOrBlank() || typedKey().isNotEmpty()) {
            refreshAccounts()
        }
    }

    override fun goneAway() {
        if (lfAccounts.isNotEmpty()) {
            saveMappings(null)
        }
        super.goneAway()
        onGoneAway?.invoke()
    }

    private fun typedKey(): String = String(keyField.password).trim()

    private fun keyForRequest(): String? {
        val typed = typedKey()
        if (typed.isNotEmpty()) return typed
        return settings?.apiKey()
    }

    private fun saveKey(): Boolean {
        val store = settings ?: run {
            setStatus("Open a data file to save the key.")
            return false
        }
        val typed = typedKey()
        if (typed.isEmpty()) {
            if (store.apiKey() != null) {
                setStatus("Using the key already saved.")
                return true
            }
            setStatus("Paste an API key first.")
            return false
        }
        store.setApiKey(typed)
        keyField.text = ""
        refreshSavedLabel()
        setStatus("Key saved.")
        return true
    }

    private fun forgetKey() {
        settings?.clearApiKey()
        keyField.text = ""
        lfAccounts = emptyList()
        mappingPanel.setLunchFlowAccounts(emptyList())
        refreshSavedLabel()
        setStatus("Key removed.")
    }

    private fun saveMappings(okMessage: String?): List<AccountMapping> {
        val maps = mappingPanel.collectMappings()
        settings?.setMappings(maps)
        mappingPanel.setSavedMappings(maps)
        if (okMessage != null) setStatus(okMessage)
        return maps
    }

    private fun refreshAccounts() {
        if (busy) return
        if (SyncService.inFlight) {
            setStatus("Import is running. The list will update when it finishes.")
            return
        }
        val key = keyForRequest()
        if (key.isNullOrEmpty()) {
            setStatus("Paste an API key first.")
            return
        }
        val typed = typedKey()
        busy = true
        setBusyButtons(false)
        setStatus("Loading accounts…")
        MdNotify.bar(mdGUI, "loading accounts", 0.15)
        MdNotify.log("refresh accounts")
        object : SwingWorker<List<LunchFlowAccount>, Void>() {
            override fun doInBackground(): List<LunchFlowAccount> = LunchFlowClient(key).listAccounts()

            override fun done() {
                busy = false
                setBusyButtons(true)
                try {
                    val accounts = get()
                    if (typed.isNotEmpty()) {
                        settings?.setApiKey(typed)
                        keyField.text = ""
                        refreshSavedLabel()
                    }
                    showAccounts(accounts)
                    val n = accounts.size
                    val text = when (n) {
                        0 -> "No accounts on this API key. Enable them in Lunch Flow under Destinations → Account Access."
                        1 -> "1 account. If you expected more, enable them under Destinations → Account Access."
                        else -> "$n accounts."
                    }
                    setStatus(text)
                    MdNotify.log("refresh: $text")
                    MdNotify.bar(mdGUI, if (n == 1) "1 account" else "$n accounts", 1.0)
                } catch (e: Exception) {
                    val cause = e.cause ?: e
                    val msg = cause.message ?: "Could not reach Lunch Flow."
                    setStatus(msg)
                    MdNotify.log("refresh failed: ${cause.javaClass.simpleName}: $msg", cause)
                    MdNotify.bar(mdGUI, msg, 0.0)
                }
            }
        }.execute()
    }

    private fun syncNow() {
        if (busy || SyncService.inFlight) return
        val store = settings
        val openBook = book
        if (store == null || openBook == null) {
            setStatus("Open a data file to import.")
            return
        }
        val key = keyForRequest()
        if (key.isNullOrEmpty()) {
            setStatus("Paste an API key first.")
            return
        }
        val typed = typedKey()
        if (typed.isNotEmpty()) {
            store.setApiKey(typed)
            keyField.text = ""
            refreshSavedLabel()
        }
        val maps = saveMappings("Saving mappings…")
        SyncService.start(
            book = openBook,
            settings = store,
            gui = mdGUI,
            mappings = maps,
            key = key,
            onStatus = { showStatus(it) },
            onBusy = { setImportBusy(it) },
            onAccounts = { showAccounts(it) },
            onMappings = { showSavedMappings(it) }
        )
    }

    fun showStatus(text: String) {
        setStatus(text)
    }

    fun setImportBusy(running: Boolean) {
        busy = running
        setBusyButtons(!running)
    }

    fun showSavedMappings(mappings: List<AccountMapping>) {
        mappingPanel.setSavedMappings(mappings)
    }

    fun showAccounts(accounts: List<LunchFlowAccount>) {
        lfAccounts = accounts
        val saved = settings?.mappings().orEmpty()
        val named = saved.map { mapping ->
            val lf = accounts.firstOrNull { it.id == mapping.lunchFlowAccountId } ?: return@map mapping
            mapping.withLunchFlow(lf)
        }
        if (named != saved) settings?.setMappings(named)
        mappingPanel.setSavedMappings(named)
        mappingPanel.setLunchFlowAccounts(accounts)
    }

    private fun showSavedAccountRows() {
        val maps = settings?.mappings().orEmpty()
        if (maps.isEmpty()) return
        showAccounts(
            maps.map { mapping ->
                LunchFlowAccount(
                    id = mapping.lunchFlowAccountId,
                    connectionId = null,
                    name = mapping.lunchFlowName?.ifBlank { null } ?: "Account ${mapping.lunchFlowAccountId}",
                    institutionName = mapping.institutionName.orEmpty(),
                    provider = null,
                    currency = null,
                    status = "ACTIVE"
                )
            }
        )
    }

    private fun refreshSavedLabel() {
        val stored = settings?.apiKey()
        savedLabel.text = if (stored == null) {
            "Not saved yet."
        } else {
            "Saved ${ApiKeyMask.lastFour(stored)}. Leave blank to keep it."
        }
    }

    private fun setBusyButtons(enabled: Boolean) {
        val hasFile = settings != null && book != null
        val on = enabled && hasFile
        keyField.isEnabled = hasFile
        refreshButton.isEnabled = on
        saveButton.isEnabled = on
        forgetButton.isEnabled = on
        syncButton.isEnabled = on
        importOnOpenBox.isEnabled = hasFile
    }

    private fun setStatus(text: String) {
        statusArea.text = text
        statusArea.caretPosition = 0
    }

    companion object {
        const val DOCS_URL: String =
            "https://github.com/dvdoug/moneydance-lunchflow/blob/master/docs/user/setup.md"
    }
}
