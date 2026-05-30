package com.example.yourhour

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class TodoItem(
    val id: String,
    val title: String,
    val reminderTime: String,
    val isDone: Boolean = false
)

// FIX: আলাদা dataStore
private val Context.todoDataStore by preferencesDataStore(name = "todos")

class TodoManager(private val context: Context) {

    private val TODO_KEY = stringPreferencesKey("todos")

    val todosFlow: Flow<List<TodoItem>> = context.todoDataStore.data.map { preferences ->
        val json = preferences[TODO_KEY] ?: "[]"
        parseTodos(json)
    }

    suspend fun addTodo(title: String, reminderTime: String) {
        context.todoDataStore.edit { preferences ->
            val json  = preferences[TODO_KEY] ?: "[]"
            val array = JSONArray(json)
            array.put(JSONObject().apply {
                put("id",           System.currentTimeMillis().toString())
                put("title",        title)
                put("reminderTime", reminderTime)
                put("isDone",       false)
            })
            preferences[TODO_KEY] = array.toString()
        }
    }

    suspend fun markDone(id: String) {
        context.todoDataStore.edit { preferences ->
            val json     = preferences[TODO_KEY] ?: "[]"
            val array    = JSONArray(json)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") == id) obj.put("isDone", true)
                newArray.put(obj)
            }
            preferences[TODO_KEY] = newArray.toString()
        }
    }

    suspend fun deleteTodo(id: String) {
        context.todoDataStore.edit { preferences ->
            val json     = preferences[TODO_KEY] ?: "[]"
            val array    = JSONArray(json)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") != id) newArray.put(obj)
            }
            preferences[TODO_KEY] = newArray.toString()
        }
    }

    private fun parseTodos(json: String): List<TodoItem> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                TodoItem(
                    id           = obj.getString("id"),
                    title        = obj.getString("title"),
                    reminderTime = obj.getString("reminderTime"),
                    isDone       = obj.getBoolean("isDone")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}