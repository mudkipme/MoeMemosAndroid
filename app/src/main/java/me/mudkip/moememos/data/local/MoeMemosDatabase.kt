package me.mudkip.moememos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity

@Database(
    entities = [MemoEntity::class, ResourceEntity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class MoeMemosDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: MoeMemosDatabase? = null

        fun getDatabase(context: Context): MoeMemosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoeMemosDatabase::class.java,
                    "moememos_database_localfirst"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
