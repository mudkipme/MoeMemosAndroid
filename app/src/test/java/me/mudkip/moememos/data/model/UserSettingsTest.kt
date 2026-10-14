package me.mudkip.moememos.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun oldJsonWithoutField_defaultsToFalse() {
        val s = json.decodeFromString<UserSettings>("""{"draft":"x","autosave":true}""")
        assertFalse(s.searchIncludesArchived)
        assertTrue(s.autosave)
    }

    @Test
    fun fieldRoundTrips() {
        val s = UserSettings(searchIncludesArchived = true)
        assertEquals(s, json.decodeFromString<UserSettings>(json.encodeToString(s)))
    }
}
