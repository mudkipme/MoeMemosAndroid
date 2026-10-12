package me.mudkip.moememos.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.mudkip.moememos.data.api.MemosV1Memo
import org.junit.Assert.*
import org.junit.Test

class MemoLocationTest {
    @Test fun acceptsZeroCoordinatesBoundariesAndDecimalCommas() {
        assertEquals(MemoLocation(" Origin ", 0.0, 0.0), MemoLocation.fromInput(" Origin ", "0", "0"))
        assertEquals(MemoLocation("", 0.0, 0.0), MemoLocation.fromInput("", "0", "0"))
        assertEquals(MemoLocation("South", -90.0, -180.0), MemoLocation.fromInput("South", "-90", "-180"))
        assertEquals(MemoLocation("North", 90.0, 180.0), MemoLocation.fromInput("North", "90", "180"))
        assertEquals(MemoLocation("Home", 1.25, -2.5), MemoLocation.fromInput("Home", "1,25", "-2,5"))
    }

    @Test fun rejectsMissingInvalidNonfiniteAndOutOfRangeCoordinates() {
        for (value in listOf("", "abc", "NaN", "Infinity", "-Infinity", "90.1", "-90.1")) {
            assertNull(MemoLocation.fromInput("Place", value, "0"))
        }
        for (value in listOf("", "abc", "NaN", "Infinity", "180.1", "-180.1")) {
            assertNull(MemoLocation.fromInput("Place", "0", value))
        }
        assertEquals(MemoLocation("  ", 0.0, 0.0), MemoLocation.fromInput("  ", "0", "0"))
    }

    @Test fun readsMemosLocationIncludingOmittedProtobufDefaults() {
        val memo = Json.decodeFromString<MemosV1Memo>("""{"name":"memos/a","location":{"placeholder":"Origin"}}""")
        assertEquals(MemoLocation("Origin", 0.0, 0.0), memo.location)
        assertNull(Json.decodeFromString<MemosV1Memo>("""{"name":"memos/a"}""").location)
        assertEquals(memo, Json.decodeFromString<MemosV1Memo>(Json.encodeToString(memo)))
    }

    @Test fun coordinateFallbackIsStableForEmptyLabels() {
        assertEquals("-1.250000°, 2.500000°", MemoLocation(" ", -1.25, 2.5).displayText)
        assertEquals("Home", MemoLocation(" Home ", -1.25, 2.5).displayText)
    }

    @Test fun oldSettingsHaveNoLocationAndNewDraftsRoundTrip() {
        assertNull(Json.decodeFromString<UserSettings>("""{"draft":"old note"}""").draftLocation)
        val settings = UserSettings(draft = "note", draftLocation = MemoLocation("Home", 1.0, -1.0))
        assertEquals(settings, Json.decodeFromString<UserSettings>(Json.encodeToString(settings)))
    }
}
