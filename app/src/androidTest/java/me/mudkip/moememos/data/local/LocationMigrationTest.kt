package me.mudkip.moememos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.mudkip.moememos.data.model.MemoLocation
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LocationMigrationTest {
    @Test fun migratesExistingNotesAndAttachmentsWithoutLosingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "location-migration-${UUID.randomUUID()}"
        val testContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
        val schema = testContext.assets.open("me.mudkip.moememos.data.local.MoeMemosDatabase/1.json").bufferedReader().use {
            Json.parseToJsonElement(it.readText()).jsonObject.getValue("database").jsonObject
        }
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val definition = entity.jsonObject
                val tableName = definition.getValue("tableName").jsonPrimitive.content
                db.execSQL(definition.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", tableName))
                definition.getValue("indices").jsonArray.forEach { index ->
                    db.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", tableName))
                }
            }
            db.execSQL("INSERT INTO memos(identifier, accountKey, content, date, visibility, pinned, archived, needsSync, isDeleted, lastModified) VALUES ('old', 'local', 'keep me', 1000, 'PRIVATE', 1, 0, 0, 0, 1000)")
            db.execSQL("INSERT INTO resources(identifier, accountKey, date, filename, uri, memoId) VALUES ('file', 'local', 1000, 'file.txt', 'file:///test', 'old')")
            db.version = 1
        }
        val database = Room.databaseBuilder(context, MoeMemosDatabase::class.java, name)
            .addMigrations(MoeMemosDatabase.MIGRATION_1_2).build()
        try {
            val memo = requireNotNull(database.memoDao().getMemoById("old", "local"))
            assertEquals("keep me", memo.content)
            assertTrue(memo.pinned)
            assertNull(memo.location)
            assertEquals("file", database.memoDao().getMemoResources("old", "local").single().identifier)
            val location = MemoLocation("Origin", 0.0, 0.0)
            database.memoDao().insertMemo(memo.copy(location = location))
            assertEquals(location, database.memoDao().getMemoById("old", "local")?.location)
            database.memoDao().insertMemo(memo.copy(location = null))
            assertNull(database.memoDao().getMemoById("old", "local")?.location)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
