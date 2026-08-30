package com.moneydance.modules.features.lunchflow.settings

object ApiKeyMask {
    fun lastFour(key: String): String {
        val trimmed = key.trim()
        if (trimmed.length < 4) return "••••"
        return "••••" + trimmed.takeLast(4)
    }
}
