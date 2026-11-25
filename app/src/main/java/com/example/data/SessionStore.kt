package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.somnathSessionStore by preferencesDataStore(name = "somnath_player_session")

class SessionStore(context: Context) {
    private val appContext = context.applicationContext

    val activeMobileFlow: Flow<String?> = appContext.somnathSessionStore.data
        .map { prefs -> prefs[KEY_ACTIVE_MOBILE] }

    val hasAdminPinFlow: Flow<Boolean> = appContext.somnathSessionStore.data
        .map { prefs ->
            !prefs[KEY_ADMIN_PIN_SALT].isNullOrBlank() && !prefs[KEY_ADMIN_PIN_HASH].isNullOrBlank()
        }

    suspend fun saveSession(mobileTenDigits: String) {
        appContext.somnathSessionStore.edit { prefs ->
            prefs[KEY_ACTIVE_MOBILE] = mobileTenDigits
        }
    }

    suspend fun clearSession() {
        appContext.somnathSessionStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_MOBILE)
        }
    }

    suspend fun getAdminPinCredentials(): Pair<String, String>? {
        val prefs = appContext.somnathSessionStore.data.firstOrNull() ?: return null
        val salt = prefs[KEY_ADMIN_PIN_SALT]
        val hash = prefs[KEY_ADMIN_PIN_HASH]
        return if (!salt.isNullOrBlank() && !hash.isNullOrBlank()) salt to hash else null
    }

    suspend fun saveAdminPinCredentials(saltHex: String, hashHex: String) {
        appContext.somnathSessionStore.edit { prefs ->
            prefs[KEY_ADMIN_PIN_SALT] = saltHex
            prefs[KEY_ADMIN_PIN_HASH] = hashHex
        }
    }

    private companion object {
        val KEY_ACTIVE_MOBILE = stringPreferencesKey("active_mobile_number")
        val KEY_ADMIN_PIN_SALT = stringPreferencesKey("admin_pin_salt")
        val KEY_ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
    }
}
