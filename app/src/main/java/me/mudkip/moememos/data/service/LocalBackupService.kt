package me.mudkip.moememos.data.service

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.LocalBackupArchive
import me.mudkip.moememos.data.local.LocalBackupAttachment
import me.mudkip.moememos.data.local.LocalBackupData
import me.mudkip.moememos.data.local.LocalBackupManifest
import me.mudkip.moememos.data.local.LocalBackupMemo
import me.mudkip.moememos.data.local.MoeMemosDatabase
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.widget.WidgetUpdater
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class LocalImportPreview(val memos: Int, val attachments: Int, val existingMemos: Int, val legacy: Boolean, val ignoredFiles: Int)
data class LocalImportResult(val importedMemos: Int, val skippedMemos: Int, val importedAttachments: Int, val skippedAttachments: Int)

class PreparedLocalImport internal constructor(
    internal val directory: File,
    internal val data: LocalBackupData,
    val preview: LocalImportPreview,
) : Closeable {
    override fun close() { directory.deleteRecursively() }
}

/** Local-account-only backup and restore. Import never uses the currently selected repository. */
@Singleton
class LocalBackupService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: MoeMemosDatabase,
    private val fileStorage: FileStorage,
) {
    private val accountKey = Account.Local().accountKey()
    private val importMutex = Mutex()

    suspend fun export(destination: Uri) = withContext(Dispatchers.IO) {
        val coroutineContext = currentCoroutineContext()
        val dao = database.memoDao()
        val data = database.withTransaction {
            val memos = dao.getAllMemosForSync(accountKey).filterNot { it.isDeleted }.sortedBy { it.date }
            val memoIds = memos.mapTo(hashSetOf()) { it.identifier }
            val resources = dao.getAllResources(accountKey).filter { it.memoId == null || it.memoId in memoIds }
            val memoRecords = memos.mapIndexed { index, memo ->
                LocalBackupMemo(memo.identifier, "memos/$index.md", memo.date.toString(), memo.lastModified.toString(),
                    memo.visibility.name, memo.pinned, memo.archived)
            }
            val attachmentRecords = resources.mapIndexed { index, resource ->
                LocalBackupAttachment(resource.identifier, resource.memoId, resource.filename, resource.mimeType,
                    resource.date.toString(), "attachments/$index")
            }
            LocalBackupData(
                LocalBackupManifest(exportedAt = Instant.now().toString(), memos = memoRecords, attachments = attachmentRecords),
                memoRecords.zip(memos).associate { (record, memo) -> record.contentFile to memo.content },
                attachmentRecords.zip(resources).associate { (record, resource) ->
                    val uri = (resource.localUri ?: resource.uri).toUri()
                    require(uri.scheme == "file" && uri.path != null) { "Missing local attachment: ${resource.filename}" }
                    record.file to File(requireNotNull(uri.path))
                },
            )
        }
        // Build a complete archive before opening the user-selected destination.
        val archive = File.createTempFile("local-export-", ".zip", context.cacheDir)
        try {
            archive.outputStream().use { LocalBackupArchive.write(data, it) { coroutineContext.ensureActive() } }
            require(archive.length() <= LocalBackupArchive.MAX_ARCHIVE_BYTES) { "Backup is too large" }
            val output = requireNotNull(context.contentResolver.openOutputStream(destination, "wt")) { "Cannot open export destination" }
            output.use { target ->
                archive.inputStream().use { input ->
                    LocalBackupArchive.copyLimited(input, target, LocalBackupArchive.MAX_ARCHIVE_BYTES) { _, _ -> coroutineContext.ensureActive() }
                }
            }
        } finally {
            archive.delete()
        }
    }

    suspend fun prepareImport(source: Uri): PreparedLocalImport {
        val directory = Files.createTempDirectory(context.cacheDir.toPath(), "local-import-").toFile()
        try {
            return withContext(Dispatchers.IO) {
                val coroutineContext = currentCoroutineContext()
                val archive = File(directory, "backup.zip")
                val input = requireNotNull(context.contentResolver.openInputStream(source)) { "Cannot open backup" }
                input.use { stream ->
                    archive.outputStream().use { output ->
                        LocalBackupArchive.copyLimited(stream, output, LocalBackupArchive.MAX_ARCHIVE_BYTES) { _, _ -> coroutineContext.ensureActive() }
                    }
                }
                val data = LocalBackupArchive.read(archive, File(directory, "attachments")) { coroutineContext.ensureActive() }
                archive.delete()
                val existing = database.memoDao().getAllMemosForSync(accountKey).mapTo(hashSetOf()) { it.identifier }
                PreparedLocalImport(directory, data, LocalImportPreview(
                    data.manifest.memos.size, data.manifest.attachments.size,
                    data.manifest.memos.count { it.id in existing }, data.legacy, data.ignoredFiles,
                ))
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { directory.deleteRecursively() }
            throw error
        }
    }

    suspend fun restore(prepared: PreparedLocalImport): LocalImportResult = withContext(Dispatchers.IO) {
        importMutex.withLock {
            // Once copying starts, finish or roll back even if the screen goes away. New file names
            // never overwrite existing files. The database commit includes every restored row.
            withContext(NonCancellable) {
                val createdFiles = mutableListOf<Uri>()
                var committed = false
                try {
                    val result = database.withTransaction {
                        val dao = database.memoDao()
                        val data = prepared.data
                        val existing = dao.getAllMemosForSync(accountKey).mapTo(hashSetOf()) { it.identifier }
                        val pending = data.manifest.memos.filterNot { it.id in existing }
                        val pendingIds = pending.mapTo(hashSetOf()) { it.id }
                        pending.forEach { memo ->
                            dao.insertImportedMemo(MemoEntity(
                                identifier = memo.id, accountKey = accountKey,
                                content = data.contents.getValue(memo.contentFile), date = Instant.parse(memo.createdAt),
                                visibility = MemoVisibility.valueOf(memo.visibility), pinned = memo.pinned, archived = memo.archived,
                                needsSync = false, lastModified = Instant.parse(memo.updatedAt), lastSyncedAt = null,
                            ))
                        }
                        var attachments = 0
                        data.manifest.attachments.forEach { attachment ->
                            if (attachment.memoId != null && attachment.memoId !in pendingIds) return@forEach
                            if (attachment.memoId == null && dao.getResourceById(attachment.id, accountKey) != null) return@forEach
                            // Register the intended destination before copying, so a failed copy is
                            // cleaned up too. Only generated basenames reach FileStorage.
                            val filename = "restore-${UUID.randomUUID()}"
                            val uri = fileStorage.saveFile(accountKey, ByteArray(0), filename)
                            createdFiles.add(uri)
                            val copied = prepared.data.files.getValue(attachment.file).inputStream().use { input ->
                                fileStorage.saveFile(accountKey, input, filename)
                            }
                            dao.insertImportedResource(ResourceEntity(
                                identifier = attachment.id, accountKey = accountKey, date = Instant.parse(attachment.createdAt),
                                filename = attachment.filename, uri = copied.toString(), localUri = copied.toString(),
                                mimeType = attachment.mimeType ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                    attachment.filename.substringAfterLast('.', "").lowercase(Locale.US)
                                ), memoId = attachment.memoId,
                            ))
                            attachments++
                        }
                        LocalImportResult(pending.size, data.manifest.memos.size - pending.size,
                            attachments, data.manifest.attachments.size - attachments)
                    }
                    committed = true
                    WidgetUpdater.updateWidgets(context)
                    result
                } finally {
                    if (!committed) createdFiles.forEach(fileStorage::deleteFile)
                }
            }
        }
    }
}
