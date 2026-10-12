package me.mudkip.moememos.data.local

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.doubleOrNull
import me.mudkip.moememos.data.model.MemoLocation

/** Backup records require explicit, correctly typed coordinates; API protobuf defaults differ. */
object BackupMemoLocationSerializer : JsonTransformingSerializer<MemoLocation>(MemoLocation.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val location = element as? JsonObject ?: throw SerializationException("Invalid memo location")
        val label = location["placeholder"] as? JsonPrimitive
        if (label == null || !label.isString) throw SerializationException("Location placeholder must be a string")
        coordinate(location, "latitude", -90.0..90.0)
        coordinate(location, "longitude", -180.0..180.0)
        return element
    }

    private fun coordinate(location: JsonObject, field: String, range: ClosedFloatingPointRange<Double>) {
        val number = location[field] as? JsonPrimitive
        val value = number?.takeUnless { it.isString }?.doubleOrNull
        if (value == null || !value.isFinite() || value !in range) {
            throw SerializationException("Invalid location $field")
        }
    }
}
