package com.cy.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cy.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 运行时设置快照 */
data class AppSettings(
    val selectedProviderId: String = AiProviders.all.first().id,
    val apiKeys: Map<String, String> = emptyMap(),
    /** 自定义 baseUrl，key 为 provider id，value 为空串表示使用默认 */
    val baseUrls: Map<String, String> = emptyMap(),
    /** 选中的模型，key 为 provider id */
    val models: Map<String, String> = emptyMap(),
    val deepThink: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
) {
    fun selectedProvider(): com.cy.app.data.model.AiProvider = AiProviders.byId(selectedProviderId)
    fun apiKeyOf(providerId: String): String = apiKeys[providerId].orEmpty()
    fun baseUrlOf(providerId: String): String =
        baseUrls[providerId]?.takeIf { it.isNotBlank() } ?: AiProviders.byId(providerId).defaultBaseUrl
    fun modelOf(providerId: String): String =
        models[providerId]?.takeIf { it.isNotBlank() } ?: AiProviders.byId(providerId).models.first()
}

/** 基于 DataStore 的设置持久化 */
class SettingsRepository(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val dataStore: DataStore<Preferences> get() = context.dataStore

    private fun apiKeyKey(id: String) = stringPreferencesKey("apiKey_$id")
    private fun baseUrlKey(id: String) = stringPreferencesKey("baseUrl_$id")
    private fun modelKey(id: String) = stringPreferencesKey("model_$id")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            selectedProviderId = prefs[stringPreferencesKey("selectedProviderId")] ?: AiProviders.all.first().id,
            apiKeys = AiProviders.all.associate { provider -> provider.id to (prefs[apiKeyKey(provider.id)] ?: "") },
            baseUrls = AiProviders.all.associate { provider -> provider.id to (prefs[baseUrlKey(provider.id)] ?: "") },
            models = AiProviders.all.associate { provider -> provider.id to (prefs[modelKey(provider.id)] ?: "") },
            deepThink = prefs[booleanPreferencesKey("deepThink")] ?: false,
            themeMode = ThemeMode.from(prefs[stringPreferencesKey("themeMode")]),
        )
    }

    suspend fun setSelectedProvider(id: String) {
        dataStore.edit { it[stringPreferencesKey("selectedProviderId")] = id }
    }

    suspend fun setApiKey(providerId: String, key: String) {
        dataStore.edit { it[apiKeyKey(providerId)] = key.trim() }
    }

    suspend fun setBaseUrl(providerId: String, url: String) {
        dataStore.edit { it[baseUrlKey(providerId)] = url.trim() }
    }

    suspend fun setModel(providerId: String, model: String) {
        dataStore.edit { it[modelKey(providerId)] = model }
    }

    suspend fun setDeepThink(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("deepThink")] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[stringPreferencesKey("themeMode")] = mode.key }
    }
}
