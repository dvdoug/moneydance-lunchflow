package com.moneydance.modules.features.lunchflow.api

sealed class LunchFlowException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingKey : LunchFlowException("Paste a Lunch Flow API key first.")
    class Unauthorized : LunchFlowException(
        "Lunch Flow rejected the API key. In the Lunch Flow website (a third-party service, not this extension), " +
            "open Destinations → Add Destination → API, copy that key, and paste it here."
    )
    class Forbidden : LunchFlowException(
        "Lunch Flow refused access. The key may be valid but the Lunch Flow subscription is inactive."
    )
    class NotFound : LunchFlowException("Lunch Flow could not find that account. Check Account Access on the API destination.")
    class Network(cause: Throwable) : LunchFlowException(
        "Could not reach Lunch Flow. Check your internet connection.",
        cause
    )
    class Http(val status: Int) : LunchFlowException("Lunch Flow returned HTTP $status.")
    class Parse(detail: String) : LunchFlowException("Unexpected response from Lunch Flow ($detail).")
}
