package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class JournalEntry(
    val date: String,
    val content: String
)

class JournalManager(private val context: Context) {

    private val JOURNAL_KEY = stringPreferencesKey("journal_entries")

    val entriesFlow: Flow<List<JournalEntry>> = context.dataStore.data.map { preferences ->
        val json = preferences[JOURNAL_KEY] ?: "[]"
        parseEntries(json)
    }

    suspend fun saveEntry(content: String) {
        val today = LocalDate.now().toString()
        context.dataStore.edit { preferences ->
            val json = preferences[JOURNAL_KEY] ?: "[]"
            val array = JSONArray(json)
            // আজকের entry আছে কিনা দেখো
            var found = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("date") == today) {
                    obj.put("content", content)
                    found = true
                    break
                }
            }
            if (!found) {
                array.put(JSONObject().apply {
                    put("date", today)
                    put("content", content)
                })
            }
            preferences[JOURNAL_KEY] = array.toString()
        }
    }

    fun getTodayEntry(entries: List<JournalEntry>): String {
        val today = LocalDate.now().toString()
        return entries.find { it.date == today }?.content ?: ""
    }

    private fun parseEntries(json: String): List<JournalEntry> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                JournalEntry(
                    date = obj.getString("date"),
                    content = obj.getString("content")
                )
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }
}