package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// FIX: আলাদা dataStore — "goals" store এর সাথে collision নেই
private val Context.limitDataStore by preferencesDataStore(name = "limits")

class AppLimitManager(private val context: Context) {

    fun getLimitFlow(packageName: String): Flow<Long> {
        val key = longPreferencesKey("limit_$packageName")
        return context.limitDataStore.data.map { preferences ->
            preferences[key] ?: 0L
        }
    }

    suspend fun saveLimit(packageName: String, minutes: Long) {
        val key = longPreferencesKey("limit_$packageName")
        context.limitDataStore.edit { preferences ->
            preferences[key] = minutes
        }
    }

    suspend fun clearLimit(packageName: String) {
        val key = longPreferencesKey("limit_$packageName")
        context.limitDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}