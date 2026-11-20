package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import android.util.Base64

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secureauth_prefs")

class SessionPreferences(private val context: Context) {
    private val keySessionToken = stringPreferencesKey("session_token")
    private val keyRememberMe = booleanPreferencesKey("remember_me")
    private val keyDarkMode = booleanPreferencesKey("dark_mode")
    private val keyHasThemeOverride = booleanPreferencesKey("has_theme_override")
    private val keyJwtSigningSecret = stringPreferencesKey("jwt_signing_secret")
    private val keyBackendUrl = stringPreferencesKey("backend_url")

    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        if (prefs[keyHasThemeOverride] == true) prefs[keyDarkMode] else null
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyBackendUrl] ?: "http://10.0.2.2:8080/api"
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyHasThemeOverride] = true
            prefs[keyDarkMode] = isDark
        }
    }

    suspend fun saveSession(token: String, rememberMe: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyRememberMe] = rememberMe
            if (rememberMe) {
                prefs[keySessionToken] = token
            } else {
                prefs.remove(keySessionToken)
            }
        }
    }

    suspend fun getSavedSessionToken(): String? {
        val prefs = context.dataStore.data.first()
        return if (prefs[keyRememberMe] == true) prefs[keySessionToken] else null
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(keySessionToken)
            prefs[keyRememberMe] = false
        }
    }

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[keyBackendUrl] = url.trim()
        }
    }

    suspend fun getOrCreateJwtSecretBytes(): ByteArray {
        val prefs = context.dataStore.data.first()
        val existing = prefs[keyJwtSigningSecret]
        if (!existing.isNullOrBlank()) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.encodeToString(secret, Base64.NO_WRAP)
        context.dataStore.edit { it[keyJwtSigningSecret] = encoded }
        return secret
    }
}
