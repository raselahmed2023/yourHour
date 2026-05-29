package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppLimitManager(private val context: Context) {

    fun getLimitFlow(packageName: String): Flow<Long> {
        val key = longPreferencesKey("limit_$packageName")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0L
        }
    }

    suspend fun saveLimit(packageName: String, minutes: Long) {
        val key = longPreferencesKey("limit_$packageName")
        context.dataStore.edit { preferences ->
            preferences[key] = minutes
        }
    }

    suspend fun clearLimit(packageName: String) {
        val key = longPreferencesKey("limit_$packageName")
        context.dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}