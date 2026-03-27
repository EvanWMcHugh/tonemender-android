package com.tonemender.app.data.local.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session_store")

class SessionStore(private val context: Context) {

    private object Keys {
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val SESSION_REFRESH_VERSION = booleanPreferencesKey("session_refresh_version")
    }

    val isSignedInFlow: Flow<Boolean> =
        context.sessionDataStore.data.map { prefs: Preferences ->
            prefs[Keys.IS_SIGNED_IN] ?: false
        }

    val refreshTriggerFlow: Flow<Boolean> =
        context.sessionDataStore.data.map { prefs: Preferences ->
            prefs[Keys.SESSION_REFRESH_VERSION] ?: false
        }

    suspend fun isSignedIn(): Boolean {
        return context.sessionDataStore.data
            .map { it[Keys.IS_SIGNED_IN] ?: false }
            .first()
    }

    suspend fun setSignedIn(value: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = value
        }
    }

    suspend fun triggerRefresh() {
        context.sessionDataStore.edit { prefs ->
            val current = prefs[Keys.SESSION_REFRESH_VERSION] ?: false
            prefs[Keys.SESSION_REFRESH_VERSION] = !current
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}