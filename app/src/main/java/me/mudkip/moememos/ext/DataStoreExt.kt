package me.mudkip.moememos.ext

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import me.mudkip.moememos.data.model.Settings
import me.mudkip.moememos.util.SettingsSerializer

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings.json",
    serializer = SettingsSerializer
)
