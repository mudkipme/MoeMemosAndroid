package me.mudkip.moememos.util

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.mudkip.moememos.data.model.Settings
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer: Serializer<Settings> {
    override val defaultValue: Settings = Settings()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun readFrom(input: InputStream): Settings {
        return try {
            val content = input.readBytes().decodeToString()
            if (content.isBlank()) {
                defaultValue
            } else {
                json.decodeFromString<Settings>(content)
            }
        } catch (exception: SerializationException) {
            // Old protobuf format or malformed data: reset settings rather than crashing.
            defaultValue
        } catch (exception: IllegalArgumentException) {
            throw CorruptionException("Cannot read settings data.", exception)
        }
    }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
