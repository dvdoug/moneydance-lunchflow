package com.moneydance.modules.features.lunchflow.settings

import com.infinitekind.moneydance.model.AccountBook
import com.infinitekind.moneydance.model.LocalStorage

class SettingsStore(
    private val getAuth: (String) -> String?,
    private val setAuth: (String, String) -> Unit,
    private val clearAuth: (String) -> Unit,
    private val getPlain: (String) -> String? = { null },
    private val setPlain: (String, String) -> Unit = { _, _ -> },
    private val removePlain: (String) -> Unit = { }
) {
    fun apiKey(): String? {
        getPlain(API_KEY)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return getAuth(API_KEY)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun setApiKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        setAuth(API_KEY, trimmed)
        setPlain(API_KEY, trimmed)
    }

    fun clearApiKey() {
        clearAuth(API_KEY)
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
                getAuth = { storage.getCachedAuthentication(it) },
                setAuth = { key, value ->
                    storage.cacheAuthentication(key, value)
                    storage.save()
                },
                clearAuth = { key ->
                    storage.clearAuthenticationCache(key)
                    storage.save()
                },
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
