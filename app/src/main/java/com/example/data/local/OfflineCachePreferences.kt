package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.offlineDataStore by preferencesDataStore(name = "parijoto_offline_cache")

class OfflineCachePreferences(private val context: Context) {

    private val downloadedModulesKey = stringSetPreferencesKey("downloaded_module_ids")

    val downloadedModuleIds: Flow<Set<String>> = context.offlineDataStore.data
        .map { prefs -> prefs[downloadedModulesKey] ?: emptySet() }

    suspend fun markModuleDownloaded(moduleId: String) {
        context.offlineDataStore.edit { prefs ->
            val current = prefs[downloadedModulesKey] ?: emptySet()
            prefs[downloadedModulesKey] = current + moduleId
        }
    }

    suspend fun removeDownloadedModule(moduleId: String) {
        context.offlineDataStore.edit { prefs ->
            val current = prefs[downloadedModulesKey] ?: emptySet()
            prefs[downloadedModulesKey] = current - moduleId
        }
    }
}
