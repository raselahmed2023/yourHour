package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "goals")

class GoalManager(private val context: Context) {

    companion object {
        val DAILY_GOAL = longPreferencesKey("daily_goal")
    }

    val dailyGoalFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[DAILY_GOAL] ?: 4L
    }

    suspend fun saveDailyGoal(hours: Long) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_GOAL] = hours
        }
    }
}