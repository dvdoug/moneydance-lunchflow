package com.moneydance.modules.features.lunchflow.settings

import com.infinitekind.moneydance.model.AccountBook
import com.infinitekind.moneydance.model.LocalStorage

class SettingsStore(
    private val getPlain: (String) -> String? = { null },
    private val setPlain: (String, String) -> Unit = { _, _ -> },
    private val removePlain: (String) -> Unit = { }
) {
    fun apiKey(): String? = getPlain(API_KEY)?.trim()?.takeIf { it.isNotEmpty() }

    fun setApiKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        setPlain(API_KEY, trimmed)
    }

    fun clearApiKey() {
        removePlain(API_KEY)
    }

    fun mappings(): List<AccountMapping> = AccountMappingCodec.fromJson(getPlain(MAPPINGS))

    fun setMappings(mappings: List<AccountMapping>) {
        setPlain(MAPPINGS, AccountMappingCodec.toJson(mappings))
    }

    fun importOnOpen(): Boolean = getPlain(IMPORT_ON_OPEN) == "true"

    fun setImportOnOpen(enabled: Boolean) {
        setPlain(IMPORT_ON_OPEN, if (enabled) "true" else "false")
    }

    companion object {
        const val API_KEY: String = "lunchflow.apiKey"
        const val MAPPINGS: String = "lunchflow.mappings"
        const val IMPORT_ON_OPEN: String = "lunchflow.importOnOpen"

        fun fromBook(book: AccountBook?): SettingsStore? {
            val storage: LocalStorage = book?.localStorage ?: return null
            return SettingsStore(
                getPlain = { storage[it] },
                setPlain = { key, value ->
                    storage.put(key, value)
                    storage.save()
                },
                removePlain = { key ->
                    storage.remove(key)
                    storage.save()
                }
            )
        }
    }
}
