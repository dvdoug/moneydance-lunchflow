package com.moneydance.modules.features.lunchflow

import com.moneydance.apps.md.controller.FeatureModule
import com.moneydance.apps.md.controller.FeatureModuleContext
import com.moneydance.apps.md.controller.Main as MdMain
import com.moneydance.apps.md.view.gui.MoneydanceGUI
import com.moneydance.modules.features.lunchflow.settings.SettingsStore
import com.moneydance.modules.features.lunchflow.sync.SyncService
import com.moneydance.modules.features.lunchflow.ui.LunchFlowWindow
import com.moneydance.modules.features.lunchflow.ui.MdNotify
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import javax.swing.Timer

class Main : FeatureModule() {

    private var window: LunchFlowWindow? = null
    private var openTimer: Timer? = null

    override fun init() {
        val ctx = getContext() ?: return
        try {
            ctx.registerFeature(this, SHOW, iconImage, name)
        } catch (e: Exception) {
            MdNotify.log("failed to register feature: ${e.message}", e)
        }
        MdNotify.log("initialized build $build")
    }

    override fun getName(): String = DISPLAY_NAME

    override fun getIconImage(): Image? {
        val stream = javaClass.getResourceAsStream("icon.png") ?: return super.getIconImage()
        return stream.use { ImageIO.read(it) }
    }

    override fun invoke(uri: String) {
        val command = uri.substringBefore(':').substringBefore('?')
        if (command == SHOW || command == ID || command.isEmpty()) {
            SwingUtilities.invokeLater { showWindow() }
        }
    }

    override fun handleEvent(appEvent: String) {
        when {
            appEvent.equals("md:file:opened", ignoreCase = true) ->
                SwingUtilities.invokeLater { scheduleAutoImport() }
            appEvent.equals("md:file:closing", ignoreCase = true) ||
                appEvent.equals("md:file:closed", ignoreCase = true) ->
                SwingUtilities.invokeLater {
                    cancelAutoImport()
                    closeWindow()
                }
        }
    }

    override fun cleanup() {
        unload()
    }

    override fun unload() {
        cancelAutoImport()
        closeWindow()
    }

    private fun showWindow() {
        val existing = window
        if (existing != null && existing.isDisplayable) {
            existing.bringToFront()
            return
        }
        window = null
        val gui = moneydanceGUI()
        if (gui == null) {
            MdNotify.log("UI not ready")
            return
        }
        val book = getContext()?.currentAccountBook
        val created = LunchFlowWindow(gui, book, SettingsStore.fromBook(book))
        created.onGoneAway = {
            if (window === created) window = null
        }
        window = created
        created.bringToFront()
    }

    private fun scheduleAutoImport() {
        cancelAutoImport()
        val timer = Timer(1800) { runAutoImport() }
        timer.isRepeats = false
        openTimer = timer
        timer.start()
    }

    private fun cancelAutoImport() {
        openTimer?.stop()
        openTimer = null
    }

    private fun runAutoImport() {
        val gui = moneydanceGUI() ?: return
        val book = getContext()?.currentAccountBook ?: return
        val store = SettingsStore.fromBook(book) ?: return
        if (!store.importOnOpen()) {
            MdNotify.log("skip import (disabled for this file)")
            return
        }
        val key = store.apiKey()
        if (key.isNullOrEmpty()) {
            MdNotify.log("skip import (no API key)")
            return
        }
        val maps = store.mappings()
        SyncService.start(
            book = book,
            settings = store,
            gui = gui,
            mappings = maps,
            key = key,
            onStatus = { text -> liveWindow()?.showStatus(text) },
            onBusy = { running -> liveWindow()?.setImportBusy(running) },
            onAccounts = { accounts -> liveWindow()?.showAccounts(accounts) },
            onMappings = { updated -> liveWindow()?.showSavedMappings(updated) }
        )
    }

    private fun liveWindow(): LunchFlowWindow? = window?.takeIf { it.isDisplayable }

    private fun closeWindow() {
        val current = window
        window = null
        current?.onGoneAway = null
        current?.goAway()
    }

    private fun moneydanceGUI(): MoneydanceGUI? {
        val ctx: FeatureModuleContext = getContext() ?: return null
        val mdMain = ctx as? MdMain ?: return null
        return mdMain.ui as? MoneydanceGUI
    }

    companion object {
        const val ID: String = "lunchflow"
        const val DISPLAY_NAME: String = "Lunch Flow"
        const val AUTHOR: String = "Doug Wright"
        const val SHOW: String = "show"

        const val THIRD_PARTY_DISCLAIMER: String =
            "Unofficial extension by Doug Wright. Lunch Flow is a separate third-party service."
    }
}
