package com.moneydance.modules.features.lunchflow.ui

import com.infinitekind.util.AppDebug
import com.moneydance.apps.md.view.gui.MoneydanceGUI

/** Moneydance status bar + Help → Console. Never pass an API key through here. */
object MdNotify {
    fun bar(gui: MoneydanceGUI?, text: String, progress: Double = -1.0) {
        gui?.setStatus("Lunch Flow: $text", progress)
    }

    fun log(message: String, error: Throwable? = null) {
        val line = "lunchflow: $message"
        System.err.println(line)
        if (error != null) {
            error.printStackTrace(System.err)
            AppDebug.ALL.log(line, error)
        } else {
            AppDebug.ALL.log(line)
        }
    }
}
