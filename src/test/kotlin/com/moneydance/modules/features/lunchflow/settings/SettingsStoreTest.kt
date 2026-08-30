package com.moneydance.modules.features.lunchflow.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsStoreTest {
    @Test
    fun roundTripAndClear() {
        val auth = mutableMapOf<String, String>()
        val plain = mutableMapOf<String, String>()
        val store = SettingsStore(
            getAuth = { auth[it] },
            setAuth = { k, v -> auth[k] = v },
            clearAuth = { auth.remove(it) },
            getPlain = { plain[it] },
            setPlain = { k, v -> plain[k] = v },
            removePlain = { plain.remove(it) }
        )
        assertNull(store.apiKey())
        store.setApiKey("  abc  ")
        assertEquals("abc", store.apiKey())
        assertEquals("abc", auth[SettingsStore.API_KEY])
        assertEquals("abc", plain[SettingsStore.API_KEY])
        store.clearApiKey()
        assertNull(store.apiKey())
        assertNull(auth[SettingsStore.API_KEY])
        assertNull(plain[SettingsStore.API_KEY])
    }

    @Test
    fun readsPlainWhenAuthCacheEmpty() {
        val store = SettingsStore(
            getAuth = { null },
            setAuth = { _, _ -> },
            clearAuth = { },
            getPlain = { if (it == SettingsStore.API_KEY) "from-file" else null }
        )
        assertEquals("from-file", store.apiKey())
    }

    @Test
    fun emptySetClears() {
        val auth = mutableMapOf(SettingsStore.API_KEY to "keep")
        val plain = mutableMapOf(SettingsStore.API_KEY to "keep")
        val store = SettingsStore(
            getAuth = { auth[it] },
            setAuth = { k, v -> auth[k] = v },
            clearAuth = { auth.remove(it) },
            getPlain = { plain[it] },
            setPlain = { k, v -> plain[k] = v },
            removePlain = { plain.remove(it) }
        )
        store.setApiKey("   ")
        assertNull(store.apiKey())
    }

    @Test
    fun mappingsPersist() {
        val auth = mutableMapOf<String, String>()
        val plain = mutableMapOf<String, String>()
        val store = SettingsStore(
            getAuth = { auth[it] },
            setAuth = { k, v -> auth[k] = v },
            clearAuth = { auth.remove(it) },
            getPlain = { plain[it] },
            setPlain = { k, v -> plain[k] = v }
        )
        store.setMappings(listOf(AccountMapping(1, "abc", "2026-03-01", null)))
        assertEquals("abc", store.mappings().single().moneydanceAccountUuid)
    }

    @Test
    fun importOnOpenDefaultsFalse() {
        val plain = mutableMapOf<String, String>()
        val store = SettingsStore(
            getAuth = { null },
            setAuth = { _, _ -> },
            clearAuth = { },
            getPlain = { plain[it] },
            setPlain = { k, v -> plain[k] = v }
        )
        assertEquals(false, store.importOnOpen())
        store.setImportOnOpen(true)
        assertEquals("true", plain[SettingsStore.IMPORT_ON_OPEN])
        assertEquals(true, store.importOnOpen())
        store.setImportOnOpen(false)
        assertEquals(false, store.importOnOpen())
    }
}
