package com.localchat.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val AVATAR_COLOR_KEY = stringPreferencesKey("avatar_color")
        private val LAST_SERVER_ADDRESS_KEY = stringPreferencesKey("last_server_address")
    }

    /**
     * Save username to DataStore
     */
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    /**
     * Save avatar color to DataStore
     */
    suspend fun saveAvatarColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[AVATAR_COLOR_KEY] = color
        }
    }

    /**
     * Save last successful server address
     */
    suspend fun saveLastServerAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SERVER_ADDRESS_KEY] = address
        }
    }

    /**
     * Get saved username as Flow
     */
    fun getUsername(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }
    }

    /**
     * Get saved avatar color as Flow
     */
    fun getAvatarColor(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[AVATAR_COLOR_KEY]
        }
    }

    /**
     * Get last server address as Flow
     */
    fun getLastServerAddress(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[LAST_SERVER_ADDRESS_KEY]
        }
    }

    /**
     * Clear all saved preferences
     */
    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
