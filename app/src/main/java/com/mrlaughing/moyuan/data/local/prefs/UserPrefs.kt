package com.mrlaughing.moyuan.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好设置 DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moyuan_prefs")

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore: DataStore<Preferences> = context.dataStore
    private val secureTokenStore = SecureTokenStore(context)
    private val tokenState = MutableStateFlow(secureTokenStore.readToken())

    // ---- Preference Keys ----

    private companion object {
        val KEY_WEREAD_TOKEN = stringPreferencesKey("weread_token")
        val KEY_SYNC_HOUR = intPreferencesKey("sync_hour")
        val KEY_SYNC_MINUTE = intPreferencesKey("sync_minute")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    // ---- Weread Token ----

    val wereadToken: Flow<String?> = tokenState.asStateFlow()
        .onStart { migrateLegacyTokenIfNeeded() }

    suspend fun setWereadToken(token: String?) {
        secureTokenStore.writeToken(token)
        tokenState.value = token?.takeIf { it.isNotBlank() }
        dataStore.edit { prefs ->
            prefs.remove(KEY_WEREAD_TOKEN)
        }
    }

    private suspend fun migrateLegacyTokenIfNeeded() {
        if (!tokenState.value.isNullOrBlank()) return
        val legacyToken = dataStore.data.first()[KEY_WEREAD_TOKEN]
        if (!legacyToken.isNullOrBlank()) {
            secureTokenStore.writeToken(legacyToken)
            tokenState.value = legacyToken
            dataStore.edit { it.remove(KEY_WEREAD_TOKEN) }
        }
    }

    // ---- Sync Hour ----

    val syncHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_SYNC_HOUR] ?: 8
    }

    suspend fun setSyncHour(hour: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_HOUR] = hour.coerceIn(0, 23)
        }
    }

    // ---- Sync Minute ----

    val syncMinute: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_SYNC_MINUTE] ?: 0
    }

    suspend fun setSyncMinute(minute: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    // ---- First Launch ----

    val firstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunch(first: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = first
        }
    }
}
