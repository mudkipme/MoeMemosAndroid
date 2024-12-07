package me.mudkip.moememos.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val userId = stringPreferencesKey("user_id")
    private val userName = stringPreferencesKey("user_name")

    suspend fun saveUser(id: String, name: String) {
        context.userPreferencesStore.edit { preferences ->
            preferences[userId] = id
            preferences[userName] = name
        }
    }

    suspend fun getUser(): Pair<String, String>? {
        return context.userPreferencesStore.data
            .map { preferences ->
                val id = preferences[userId]
                val name = preferences[userName]
                if (id != null && name != null) {
                    id to name
                } else null
            }.first()
    }
} 