package me.mudkip.moememos.data.local

import androidx.room.TypeConverter
import me.mudkip.moememos.data.model.MemoVisibility
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun toMemoVisibility(value: String) = enumValueOf<MemoVisibility>(value)

    @TypeConverter
    fun fromMemoVisibility(value: MemoVisibility) = value.name
} 