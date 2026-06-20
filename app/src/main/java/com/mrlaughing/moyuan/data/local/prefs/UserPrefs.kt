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
import kotlinx.coroutines.flow.map
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

    // ---- Preference Keys ----

    private companion object {
        val KEY_WEREAD_TOKEN = stringPreferencesKey("weread_token")
        val KEY_SYNC_HOUR = intPreferencesKey("sync_hour")
        val KEY_SYNC_MINUTE = intPreferencesKey("sync_minute")
        val KEY_REFRESH_MODE = stringPreferencesKey("refresh_mode")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    // ---- Weread Token ----

    val wereadToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_WEREAD_TOKEN]
    }

    suspend fun setWereadToken(token: String?) {
        dataStore.edit { prefs ->
            if (token != null) {
                prefs[KEY_WEREAD_TOKEN] = token
            } else {
                prefs.remove(KEY_WEREAD_TOKEN)
            }
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

    // ---- Refresh Mode ----

    val refreshMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_MODE] ?: "partial"
    }

    suspend fun setRefreshMode(mode: String) {
        val valid = if (mode == "full") "full" else "partial"
        dataStore.edit { prefs ->
            prefs[KEY_REFRESH_MODE] = valid
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
