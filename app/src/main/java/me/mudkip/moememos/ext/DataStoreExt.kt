package me.mudkip.moememos.ext

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

sealed class DataStoreKeys<T> {
    abstract val key: Preferences.Key<T>

    object Host : DataStoreKeys<String>() {
        override val key: Preferences.Key<String>
            get() = stringPreferencesKey("host")
    }
}