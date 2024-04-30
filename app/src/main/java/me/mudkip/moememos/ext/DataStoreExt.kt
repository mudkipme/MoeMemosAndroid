package me.mudkip.moememos.ext

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import me.mudkip.moememos.data.model.Settings
import me.mudkip.moememos.util.SettingsSerializer

val Context.legacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.pb",
    serializer = SettingsSerializer
)

sealed class DataStoreKeys<T> {
    abstract val key: Preferences.Key<T>

    data object Host : DataStoreKeys<String>() {
        override val key: Preferences.Key<String>
            get() = stringPreferencesKey("host")
    }

    data object AccessToken : DataStoreKeys<String>() {
        override val key: Preferences.Key<String>
            get() = stringPreferencesKey("accessToken")
    }

    data object Draft : DataStoreKeys<String>() {
        override val key: Preferences.Key<String>
            get() = stringPreferencesKey("draft")
    }
}