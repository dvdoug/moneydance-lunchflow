package com.moneydance.modules.features.lunchflow.sync

import kotlin.math.abs

object PendingAmount {
    fun changed(registerMinor: Long, lunchFlowMinor: Long): Boolean =
        abs(registerMinor) != abs(lunchFlowMinor)

    fun registerMinor(currentRegister: Long, lunchFlowMinor: Long): Long {
        val mag = abs(lunchFlowMinor)
        return when {
            currentRegister > 0L -> mag
            currentRegister < 0L -> -mag
            else -> lunchFlowMinor
        }
    }
}
