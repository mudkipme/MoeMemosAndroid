package me.mudkip.moememos.data.service

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.LocalBackupArchive
import me.mudkip.moememos.data.local.LocalBackupAttachment
import me.mudkip.moememos.data.local.LocalBackupData
import me.mudkip.moememos.data.local.LocalBackupManifest
import me.mudkip.moememos.data.local.LocalBackupMemo
import me.mudkip.moememos.data.local.MoeMemosDatabase
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.MemoVisibility
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class LocalBackupServiceTest {
    private lateinit var directory: File
    private lateinit var context: Context
    private lateinit var database: MoeMemosDatabase
    private lateinit var storage: FileStorage
    private lateinit var service: LocalBackupService
    // Room stores timestamps at millisecond precision.
    private val date = Instant.parse("2026-10-05T01:02:03.123Z")

    @Before fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        directory = Files.createTempDirectory(application.cacheDir.toPath(), "backup-test-").toFile()
        context = object : ContextWrapper(application) {
            override fun getFilesDir() = File(directory, "files").apply { mkdirs() }
            override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
        }
        database = Room.inMemoryDatabaseBuilder(context, MoeMemosDatabase::class.java).build()
        storage = FileStorage(context)
        service = LocalBackupService(context, database, storage)
    }

    @After fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test fun exportAndRestorePreserveLocalIdsMetadataAndAttachments() = runBlocking {
        val dao = database.memoDao()
        val memo = MemoEntity("local-memo", accountKey = "local", content = "#旅行\n- [x] done", date = date,
            visibility = MemoVisibility.PRIVATE, pinned = true, archived = true, needsSync = false, lastModified = date.plusSeconds(20))
        dao.insertMemo(memo)
        val uri = storage.saveFile("local", byteArrayOf(1, 2, 3), "original-photo")
        val resource = ResourceEntity("local-resource", accountKey = "local", date = date, filename = "photo.jpg",
            uri = uri.toString(), localUri = uri.toString(), mimeType = "image/jpeg", memoId = memo.identifier)
        dao.insertResource(resource)
        dao.insertResource(resource.copy(identifier = "unattached", memoId = null))
        val archive = Uri.fromFile(File(directory, "backup.zip"))
        service.export(archive)
        dao.deleteResourcesByAccount("local")
        dao.deleteMemosByAccount("local")
        storage.deleteAccountFiles("local")

        service.prepareImport(archive).use { prepared ->
            assertEquals(0, prepared.preview.existingMemos)
            assertEquals(LocalImportResult(1, 0, 2, 0), service.restore(prepared))
        }
        assertEquals(memo, dao.getMemoById(memo.identifier, "local"))
        val restored = dao.getMemoResources(memo.identifier, "local").single()
        assertEquals(resource.identifier, restored.identifier)
        assertEquals(resource.filename, restored.filename)
        assertEquals(resource.date, restored.date)
        assertNull(restored.remoteId)
        assertNotEquals(resource.uri, restored.uri)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(Uri.parse(restored.uri).path!!).readBytes())
        assertNull(dao.getResourceById("unattached", "local")!!.memoId)
    }

    @Test fun repeatImportSkipsExistingNotesAndPreservesLaterEdits() = runBlocking {
        val archive = makeArchive()
        val dao = database.memoDao()
        service.prepareImport(archive).use { assertEquals(1, service.restore(it).importedMemos) }
        val memo = dao.getMemoById("memo", "local")!!
        dao.insertMemo(memo.copy(content = "edited after restore", pinned = true))
        service.prepareImport(archive).use {
            assertEquals(1, it.preview.existingMemos)
            assertEquals(LocalImportResult(0, 1, 0, 1), service.restore(it))
        }
        assertEquals("edited after restore", dao.getMemoById("memo", "local")!!.content)
        assertTrue(dao.getMemoById("memo", "local")!!.pinned)
        assertEquals(1, dao.getAllResources("local").size)
    }

    @Test fun conflictingServerResourceRollsBackAllRowsAndNewFiles() = runBlocking {
        val dao = database.memoDao()
        dao.insertMemo(MemoEntity("server-memo", accountKey = "server", content = "server data", date = date,
            visibility = MemoVisibility.PRIVATE, pinned = false))
        val uri = storage.saveFile("server", byteArrayOf(99), "server-file")
        dao.insertResource(ResourceEntity("collision", accountKey = "server", date = date, filename = "server.txt",
            uri = uri.toString(), mimeType = "text/plain", memoId = "server-memo"))
        val archive = makeArchive(secondAttachmentId = "collision")
        service.prepareImport(archive).use { prepared ->
            assertTrue(runCatching { service.restore(prepared) }.isFailure)
        }
        assertTrue(dao.getAllMemosForSync("local").isEmpty())
        assertTrue(dao.getAllResources("local").isEmpty())
        assertEquals("server data", dao.getMemoById("server-memo", "server")!!.content)
        assertArrayEquals(byteArrayOf(99), File(uri.path!!).readBytes())
        assertEquals(listOf("server-file"), context.filesDir.walkTopDown().filter { it.isFile }.map { it.name }.toList())
    }

    @Test fun legacyRestoreIsRepeatableAndReadyForLocalToServerTransfer() = runBlocking {
        val archive = File(directory, "legacy.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            for ((name, content) in mapOf("20261005-090203.md" to "legacy note", "20261005-090203-1.txt" to "attachment")) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        service.prepareImport(Uri.fromFile(archive)).use {
            assertTrue(it.preview.legacy)
            assertEquals(LocalImportResult(1, 0, 1, 0), service.restore(it))
        }
        service.prepareImport(Uri.fromFile(archive)).use {
            assertEquals(LocalImportResult(0, 1, 0, 1), service.restore(it))
        }
        val memo = database.memoDao().getAllMemosForSync("local").single()
        assertNull(memo.remoteId)
        assertFalse(memo.needsSync)
        val attachment = database.memoDao().getMemoResources(memo.identifier, "local").single()
        assertTrue(File(Uri.parse(attachment.localUri).path!!).isFile)
        assertEquals("text/plain", attachment.mimeType)
    }

    @Test fun invalidArchiveLeavesNoStagedFilesOrMemos() = runBlocking {
        val file = File(directory, "invalid.zip").apply { writeText("not a zip") }
        assertTrue(runCatching { service.prepareImport(Uri.fromFile(file)) }.isFailure)
        assertTrue(context.cacheDir.listFiles().orEmpty().isEmpty())
        assertTrue(database.memoDao().getAllMemosForSync("local").isEmpty())
    }

    private fun makeArchive(secondAttachmentId: String? = null): Uri {
        val memo = LocalBackupMemo("memo", "memos/0.md", date.toString(), date.toString(), "PRIVATE", false, false)
        val file = File(directory, "attachment").apply { writeText("attachment") }
        val attachments = mutableListOf(LocalBackupAttachment("resource", memo.id, "note.txt", "text/plain", date.toString(), "attachments/0"))
        if (secondAttachmentId != null) attachments.add(attachments.single().copy(id = secondAttachmentId, file = "attachments/1"))
        val data = LocalBackupData(LocalBackupManifest(exportedAt = date.toString(), memos = listOf(memo), attachments = attachments),
            mapOf(memo.contentFile to "original"), attachments.associate { it.file to file })
        val archive = File(directory, "backup.zip")
        archive.outputStream().use { LocalBackupArchive.write(data, it) }
        return Uri.fromFile(archive)
    }
}
