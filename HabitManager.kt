package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class HabitManager(private val context: Context) {

    // আজকের habit done কিনা
    fun getHabitFlow(habitName: String): Flow<Boolean> {
        val today = LocalDate.now().toString()
        val key = stringPreferencesKey("habit_${habitName}_${today}")
        return context.dataStore.data.map { preferences ->
            preferences[key] == "done"
        }
    }

    // habit toggle — শুধু একবার done করা যাবে
    suspend fun markHabitDone(habitName: String) {
        val today = LocalDate.now().toString()
        val key = stringPreferencesKey("habit_${habitName}_${today}")
        context.dataStore.edit { preferences ->
            preferences[key] = "done"
        }
    }
    // Prayer এর জন্য toggle ← এটা যোগ করো
    suspend fun toggleHabit(habitName: String, done: Boolean) {
        val today = LocalDate.now().toString()
        val key = stringPreferencesKey("habit_${habitName}_${today}")
        context.dataStore.edit { preferences ->
            if (done) preferences[key] = "done"
            else preferences.remove(key)
        }
    }

    // streak — আগের দিন done ছিল কিনা দেখে
    fun getStreakFlow(habitName: String): Flow<Int> {
        val key = stringPreferencesKey("streak_$habitName")
        return context.dataStore.data.map { preferences ->
            val streakData = preferences[key] ?: "0|"
            val parts = streakData.split("|")
            val count = parts[0].toIntOrNull() ?: 0
            val lastDate = if (parts.size > 1) parts[1] else ""
            val yesterday = LocalDate.now().minusDays(1).toString()
            val today = LocalDate.now().toString()
            // আজকে বা গতকাল done থাকলে streak valid
            if (lastDate == today || lastDate == yesterday) count else 0
        }
    }

    // streak update
    suspend fun incrementStreak(habitName: String, currentStreak: Int) {
        val key = stringPreferencesKey("streak_$habitName")
        val today = LocalDate.now().toString()
        context.dataStore.edit { preferences ->
            preferences[key] = "${currentStreak + 1}|$today"
        }
    }
}