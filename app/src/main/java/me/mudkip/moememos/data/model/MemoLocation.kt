package me.mudkip.moememos.data.model

import androidx.room.Ignore
import java.util.Locale
import kotlinx.serialization.Serializable

/** Matches the location metadata in the Memos API. */
@Serializable
data class MemoLocation(
    val placeholder: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
) {
    @get:Ignore
    val hasValidCoordinates: Boolean
        get() = latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    @get:Ignore
    val coordinatesText: String
        get() = String.format(Locale.US, "%.6f°, %.6f°", latitude, longitude)

    @get:Ignore
    val displayText: String
        get() = placeholder.trim().ifEmpty { coordinatesText }

    companion object {
        fun fromInput(placeholder: String, latitude: String, longitude: String): MemoLocation? {
            val location = MemoLocation(
                placeholder,
                latitude.trim().replace(',', '.').toDoubleOrNull() ?: return null,
                longitude.trim().replace(',', '.').toDoubleOrNull() ?: return null,
            )
            return location.takeIf { it.hasValidCoordinates }
        }
    }
}

/** A null change leaves metadata alone; a change containing null removes the location. */
data class MemoLocationChange(val value: MemoLocation?)
