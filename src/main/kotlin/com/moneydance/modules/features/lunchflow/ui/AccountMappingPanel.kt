package com.moneydance.modules.features.lunchflow.ui

import com.infinitekind.moneydance.model.Account
import com.infinitekind.moneydance.model.AccountBook
import com.moneydance.apps.md.view.gui.MoneydanceGUI
import com.moneydance.awt.JDateField
import com.moneydance.modules.features.lunchflow.api.LunchFlowAccount
import com.moneydance.modules.features.lunchflow.settings.AccountMapping
import com.moneydance.modules.features.lunchflow.sync.MdAccess
import com.moneydance.modules.features.lunchflow.sync.MdAccounts
import com.moneydance.modules.features.lunchflow.sync.SyncEngine
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.DefaultCellEditor
import javax.swing.JComboBox
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

class AccountMappingPanel(
    book: AccountBook?,
    mdGUI: MoneydanceGUI
) : JPanel(BorderLayout(0, 8)) {

    private val mdChoices: List<AccountChoice> = listOf(AccountChoice(null)) +
        (book?.let { MdAccounts.listMappable(it) } ?: emptyList()).map { AccountChoice(it) }

    private val model = MappingTableModel(mdChoices)
    private val table = JTable(model)
    private var saved: List<AccountMapping> = emptyList()

    init {
        table.rowHeight = 26
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.fillsViewportHeight = true
        table.tableHeader.reorderingAllowed = false
        table.putClientProperty("terminateEditOnFocusLost", true)
        table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(JComboBox(mdChoices.toTypedArray()))
        table.columnModel.getColumn(2).cellEditor = FromDateEditor(mdGUI)
        table.columnModel.getColumn(2).cellRenderer = FromDateRenderer(mdGUI)
        table.columnModel.getColumn(0).preferredWidth = 280
        table.columnModel.getColumn(1).preferredWidth = 220
        table.columnModel.getColumn(2).preferredWidth = 130
        add(JScrollPane(table), BorderLayout.CENTER)
        add(
            JTextArea().apply {
                text = "From uses your Moneydance date format (click for a calendar). Clear it for all history Lunch Flow has.\n" +
                    "Missing a bank? Enable it in Lunch Flow under Destinations → Account Access, then Refresh accounts."
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
                background = null
                isOpaque = false
                border = BorderFactory.createEmptyBorder()
            },
            BorderLayout.SOUTH
        )
    }

    fun setSavedMappings(mappings: List<AccountMapping>) {
        saved = mappings
    }

    fun setLunchFlowAccounts(accounts: List<LunchFlowAccount>) {
        model.setAccounts(accounts, saved)
    }

    fun collectMappings(): List<AccountMapping> {
        if (table.isEditing) table.cellEditor.stopCellEditing()
        return model.collect(saved)
    }

    private class MappingTableModel(
        private val mdChoices: List<AccountChoice>
    ) : AbstractTableModel() {
        private val columns = arrayOf("Lunch Flow account", "Import into", "From")
        private val rows = mutableListOf<MappingRow>()

        fun setAccounts(accounts: List<LunchFlowAccount>, saved: List<AccountMapping>) {
            rows.clear()
            accounts.forEach { lf ->
                val existing = saved.firstOrNull { it.lunchFlowAccountId == lf.id }
                rows.add(
                    MappingRow(
                        lf = lf,
                        mdUuid = existing?.moneydanceAccountUuid,
                        startDate = existing?.syncStartDate ?: AccountMapping.defaultStartDate()
                    )
                )
            }
            fireTableDataChanged()
        }

        fun collect(saved: List<AccountMapping>): List<AccountMapping> {
            return rows.mapNotNull { row ->
                val uuid = row.mdUuid ?: return@mapNotNull null
                AccountMapping(
                    lunchFlowAccountId = row.lf.id,
                    moneydanceAccountUuid = uuid,
                    syncStartDate = row.startDate?.trim()?.ifBlank { null },
                    lastPostedDate = saved.firstOrNull { it.lunchFlowAccountId == row.lf.id }?.lastPostedDate,
                    lunchFlowName = row.lf.name,
                    institutionName = row.lf.institutionName.takeIf { it.isNotBlank() }
                )
            }
        }

        override fun getRowCount(): Int = rows.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex > 0

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val row = rows[rowIndex]
            return when (columnIndex) {
                0 -> {
                    val inst = row.lf.institutionName.trim()
                    if (inst.isEmpty()) row.lf.name else "${row.lf.name}  ·  $inst"
                }
                1 -> choiceFor(row.mdUuid)
                else -> row.startDate.orEmpty()
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val row = rows[rowIndex]
            when (columnIndex) {
                1 -> row.mdUuid = (aValue as? AccountChoice)?.account?.let { MdAccess.uuid(it) }
                2 -> {
                    val text = aValue?.toString()?.trim()
                    row.startDate = text?.ifBlank { null }
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }

        private fun choiceFor(uuid: String?): AccountChoice {
            if (uuid == null) return mdChoices.first()
            return mdChoices.firstOrNull { it.account != null && MdAccess.uuid(it.account) == uuid }
                ?: mdChoices.first()
        }
    }

    private data class MappingRow(
        val lf: LunchFlowAccount,
        var mdUuid: String?,
        var startDate: String?
    )

    class AccountChoice(val account: Account?) {
        override fun toString(): String = account?.let { MdAccess.fullAccountName(it) } ?: "— not mapped —"

        override fun equals(other: Any?): Boolean {
            if (other !is AccountChoice) return false
            val a = account
            val b = other.account
            return when {
                a == null && b == null -> true
                a != null && b != null -> MdAccess.uuid(a) == MdAccess.uuid(b)
                else -> false
            }
        }

        override fun hashCode(): Int = account?.let { MdAccess.uuid(it) }?.hashCode() ?: 0
    }

    private class FromDateEditor(mdGUI: MoneydanceGUI) : AbstractCellEditor(), TableCellEditor {
        private val field = JDateField(mdGUI)

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            val iso = value as? String
            if (iso.isNullOrBlank()) {
                field.text = ""
            } else {
                field.setDateInt(SyncEngine.isoToDateInt(iso))
            }
            return field
        }

        override fun getCellEditorValue(): Any {
            if (field.text.trim().isEmpty()) return ""
            val dateInt = field.parseDateInt()
            return SyncEngine.dateIntToIso(dateInt)
        }
    }

    private class FromDateRenderer(mdGUI: MoneydanceGUI) : DefaultTableCellRenderer() {
        private val format = mdGUI.preferences.shortDateFormatter

        override fun setValue(value: Any?) {
            val iso = value as? String
            text = if (iso.isNullOrBlank()) {
                "All history"
            } else {
                try {
                    format.format(SyncEngine.isoToDateInt(iso))
                } catch (_: Exception) {
                    iso
                }
            }
        }
    }
}
