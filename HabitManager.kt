package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// FIX: আলাদা dataStore — "goals" store এর সাথে collision নেই
private val Context.habitDataStore by preferencesDataStore(name = "habits")

class HabitManager(private val context: Context) {

    // আজকের habit done কিনা
    fun getHabitFlow(habitName: String): Flow<Boolean> {
        val today = LocalDate.now().toString()
        val key = stringPreferencesKey("habit_${habitName}_${today}")
        return context.habitDataStore.data.map { preferences ->
            preferences[key] == "done"
        }
    }

    // habit একবারই done করা যাবে
    suspend fun markHabitDone(habitName: String) {
        val today = LocalDate.now().toString()
        val key = stringPreferencesKey("habit_${habitName}_${today}")
        context.habitDataStore.edit { preferences ->
            preferences[key] = "done"
        }
    }

    // Prayer এর জন্য toggle — on/off করা যাবে
    suspend fun toggleHabit(habitName: String, done: Boolean) {
        val today = LocalDate.now().toString()
        val key = stringPreferencesKey("habit_${habitName}_${today}")
        context.habitDataStore.edit { preferences ->
            if (done) preferences[key] = "done"
            else preferences.remove(key)
        }
    }

    // streak — আজকে বা গতকাল done থাকলে valid
    fun getStreakFlow(habitName: String): Flow<Int> {
        val key = stringPreferencesKey("streak_$habitName")
        return context.habitDataStore.data.map { preferences ->
            val streakData = preferences[key] ?: "0|"
            val parts      = streakData.split("|")
            val count      = parts[0].toIntOrNull() ?: 0
            val lastDate   = if (parts.size > 1) parts[1] else ""
            val yesterday  = LocalDate.now().minusDays(1).toString()
            val today      = LocalDate.now().toString()
            if (lastDate == today || lastDate == yesterday) count else 0
        }
    }

    // streak বাড়াও
    suspend fun incrementStreak(habitName: String, currentStreak: Int) {
        val key   = stringPreferencesKey("streak_$habitName")
        val today = LocalDate.now().toString()
        context.habitDataStore.edit { preferences ->
            preferences[key] = "${currentStreak + 1}|$today"
        }
    }
}