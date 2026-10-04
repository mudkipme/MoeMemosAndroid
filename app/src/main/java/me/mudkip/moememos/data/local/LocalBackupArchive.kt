package me.mudkip.moememos.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@Serializable
data class LocalBackupManifest(
    @Required val format: String = "moe-memos-local",
    @Required val version: Int = 1,
    val exportedAt: String,
    val memos: List<LocalBackupMemo>,
    val attachments: List<LocalBackupAttachment>,
)

@Serializable
data class LocalBackupMemo(
    val id: String,
    val contentFile: String,
    val createdAt: String,
    val updatedAt: String,
    val visibility: String,
    val pinned: Boolean,
    val archived: Boolean,
)

@Serializable
data class LocalBackupAttachment(
    val id: String,
    val memoId: String?,
    val filename: String,
    val mimeType: String?,
    val createdAt: String,
    val file: String,
    val size: Long = 0,
    val sha256: String = "",
)

data class LocalBackupData(
    val manifest: LocalBackupManifest,
    val contents: Map<String, String>,
    val files: Map<String, File>,
    val legacy: Boolean = false,
    val ignoredFiles: Int = 0,
)

/** Plain ZIP/Markdown plus a Moe Memos manifest. No server API or database representation. */
object LocalBackupArchive {
    private const val MANIFEST = "manifest.json"
    private const val MAX_ENTRIES = 100_000
    private const val MAX_MANIFEST_BYTES = 16L * 1024 * 1024
    private const val MAX_MEMO_BYTES = 4L * 1024 * 1024
    private const val MAX_TEXT_BYTES = 64L * 1024 * 1024
    private const val MAX_EXPANDED_BYTES = 8L * 1024 * 1024 * 1024
    const val MAX_ARCHIVE_BYTES = 4L * 1024 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val legacyMemoName = Regex("^(\\d{8}-\\d{6}(?:_\\d+)?)\\.md$")
    private val legacyAttachmentName = Regex("^(\\d{8}-\\d{6}(?:_\\d+)?)-\\d+(?:\\.[^/]*)?$")
    private val legacyDate = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss", Locale.US)
        .withResolverStyle(ResolverStyle.STRICT)

    fun write(data: LocalBackupData, output: OutputStream, checkCancelled: () -> Unit = {}) {
        val manifest = data.manifest.copy(attachments = data.manifest.attachments.map { attachment ->
            val file = data.files.getValue(attachment.file)
            require(file.isFile) { "Missing attachment: ${attachment.filename}" }
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                copyLimited(input, object : OutputStream() {
                    override fun write(value: Int) = Unit
                    override fun write(bytes: ByteArray, offset: Int, length: Int) = Unit
                }, MAX_EXPANDED_BYTES) { bytes, count ->
                    checkCancelled()
                    digest.update(bytes, 0, count)
                }
            }
            attachment.copy(size = file.length(), sha256 = digest.digest().hex())
        })
        validateManifest(manifest)
        val metadata = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
        require(metadata.size <= MAX_MANIFEST_BYTES) { "Backup metadata is too large" }
        var textBytes = 0L
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(metadata)
            zip.closeEntry()
            manifest.memos.forEach { memo ->
                checkCancelled()
                val content = data.contents.getValue(memo.contentFile).toByteArray(Charsets.UTF_8)
                textBytes += content.size
                require(content.size <= MAX_MEMO_BYTES && textBytes <= MAX_TEXT_BYTES) { "Memo content is too large" }
                zip.putNextEntry(ZipEntry(memo.contentFile))
                zip.write(content)
                zip.closeEntry()
            }
            manifest.attachments.forEach { attachment ->
                checkCancelled()
                zip.putNextEntry(ZipEntry(attachment.file))
                val digest = MessageDigest.getInstance("SHA-256")
                val size = data.files.getValue(attachment.file).inputStream().use { input ->
                    copyLimited(input, zip, attachment.size) { bytes, count ->
                        checkCancelled()
                        digest.update(bytes, 0, count)
                    }
                }
                require(size == attachment.size && digest.digest().hex() == attachment.sha256) {
                    "Attachment changed while exporting: ${attachment.filename}"
                }
                zip.closeEntry()
            }
        }
    }

    /** Files are staged under generated names; archive paths are never used as filesystem paths. */
    fun read(
        archive: File,
        attachmentDirectory: File,
        legacyZone: ZoneId = ZoneId.systemDefault(),
        checkCancelled: () -> Unit = {},
    ): LocalBackupData = ZipFile(archive).use { zip ->
        val entries = linkedMapOf<String, ZipEntry>()
        val names = hashSetOf<String>()
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            checkCancelled()
            val entry = enumeration.nextElement()
            validatePath(entry.name.removeSuffix("/"))
            require(names.add(entry.name) && names.size <= MAX_ENTRIES) { "Duplicate entries or too many files in backup" }
            if (!entry.isDirectory) entries[entry.name] = entry
        }
        var expandedBytes = 0L
        var textBytes = 0L
        fun copyEntry(entry: ZipEntry, output: OutputStream, limit: Long): Pair<Long, String> {
            val digest = MessageDigest.getInstance("SHA-256")
            val crc = CRC32()
            val size = zip.getInputStream(entry).use { input ->
                copyLimited(input, output, limit) { bytes, count ->
                    checkCancelled()
                    expandedBytes += count
                    require(expandedBytes <= MAX_EXPANDED_BYTES) { "Backup is too large to restore" }
                    digest.update(bytes, 0, count)
                    crc.update(bytes, 0, count)
                }
            }
            require(size == entry.size && crc.value == entry.crc) { "Damaged backup entry: ${entry.name}" }
            return size to digest.digest().hex()
        }
        fun text(entry: ZipEntry, limit: Long, memoContent: Boolean = true): String {
            val output = ByteArrayOutputStream()
            copyEntry(entry, output, limit)
            if (memoContent) textBytes += output.size()
            require(textBytes <= MAX_TEXT_BYTES) { "Backup contains too much text" }
            return Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(output.toByteArray())).toString()
        }
        attachmentDirectory.mkdirs()
        fun stage(entry: ZipEntry): Triple<File, Long, String> {
            val target = File.createTempFile("attachment-", ".bin", attachmentDirectory)
            val (size, hash) = target.outputStream().use { copyEntry(entry, it, MAX_EXPANDED_BYTES) }
            return Triple(target, size, hash)
        }

        val manifestEntry = entries[MANIFEST]
        if (manifestEntry != null) {
            val manifest = json.decodeFromString<LocalBackupManifest>(text(manifestEntry, MAX_MANIFEST_BYTES, memoContent = false))
            validateManifest(manifest)
            val contents = manifest.memos.associate { memo ->
                val entry = requireNotNull(entries[memo.contentFile]) { "Missing memo: ${memo.contentFile}" }
                memo.contentFile to text(entry, MAX_MEMO_BYTES)
            }
            val files = manifest.attachments.associate { attachment ->
                val entry = requireNotNull(entries[attachment.file]) { "Missing attachment: ${attachment.filename}" }
                val (file, size, hash) = stage(entry)
                require(size == attachment.size && hash == attachment.sha256) { "Damaged attachment: ${attachment.filename}" }
                attachment.file to file
            }
            LocalBackupData(manifest, contents, files,
                ignoredFiles = entries.size - 1 - contents.size - files.size)
        } else {
            val contents = linkedMapOf<String, String>()
            val byBase = linkedMapOf<String, LocalBackupMemo>()
            entries.forEach { (name, entry) ->
                val match = legacyMemoName.matchEntire(name) ?: return@forEach
                val content = text(entry, MAX_MEMO_BYTES)
                val base = match.groupValues[1]
                val date = runCatching {
                    LocalDateTime.parse(base.substringBefore('_'), legacyDate).atZone(legacyZone).toInstant()
                }.getOrElse { if (entry.time >= 0) Instant.ofEpochMilli(entry.time) else Instant.EPOCH }
                val id = legacyIdentifier("memo:$name:${sha256(content.toByteArray(Charsets.UTF_8))}")
                byBase[base] = LocalBackupMemo(id, name, date.toString(), date.toString(), "PRIVATE", false, false)
                contents[name] = content
            }
            require(byBase.isNotEmpty()) { "This ZIP is not a Moe Memos local backup" }
            val files = linkedMapOf<String, File>()
            val attachments = entries.mapNotNull { (name, entry) ->
                val match = legacyAttachmentName.matchEntire(name) ?: return@mapNotNull null
                val memo = byBase[match.groupValues[1]] ?: return@mapNotNull null
                val (file, size, hash) = stage(entry)
                files[name] = file
                LocalBackupAttachment(legacyIdentifier("attachment:${memo.id}:$name:$hash"), memo.id,
                    name, null, memo.createdAt, name, size, hash)
            }
            LocalBackupData(
                LocalBackupManifest(exportedAt = Instant.now().toString(), memos = byBase.values.toList(), attachments = attachments),
                contents, files, legacy = true, ignoredFiles = entries.size - contents.size - files.size,
            )
        }
    }

    private fun validateManifest(manifest: LocalBackupManifest) {
        require(manifest.format == "moe-memos-local") { "This is not a Moe Memos local backup" }
        require(manifest.version == 1) { "Unsupported local backup version: ${manifest.version}" }
        Instant.parse(manifest.exportedAt)
        require(manifest.memos.size + manifest.attachments.size + 1 <= MAX_ENTRIES) { "Too many files in backup" }
        val memoIds = hashSetOf<String>()
        val attachmentIds = hashSetOf<String>()
        val paths = hashSetOf(MANIFEST)
        manifest.memos.forEach { memo ->
            validateId(memo.id)
            require(memoIds.add(memo.id)) { "Duplicate memo identifier" }
            validatePath(memo.contentFile)
            require(paths.add(memo.contentFile)) { "Duplicate content path" }
            Instant.parse(memo.createdAt)
            Instant.parse(memo.updatedAt)
            require(memo.visibility in setOf("PRIVATE", "PROTECTED", "PUBLIC", "SPACE")) { "Unknown memo visibility" }
        }
        var attachmentBytes = 0L
        manifest.attachments.forEach { attachment ->
            validateId(attachment.id)
            require(attachmentIds.add(attachment.id)) { "Duplicate attachment identifier" }
            require(attachment.memoId == null || attachment.memoId in memoIds) { "Attachment refers to a missing memo" }
            validatePath(attachment.file)
            require(paths.add(attachment.file)) { "Duplicate attachment path" }
            // Preserve filename punctuation (for example colons), but disallow paths because
            // other attachment operations, including transfer, also use this basename.
            require(attachment.filename.isNotBlank() && attachment.filename !in setOf(".", "..") &&
                attachment.filename.none { it == '/' || it == '\\' || it == '\u0000' }) { "Invalid attachment filename" }
            Instant.parse(attachment.createdAt)
            require(attachment.size in 0..MAX_EXPANDED_BYTES) { "Invalid attachment size" }
            attachmentBytes += attachment.size
            require(attachmentBytes <= MAX_EXPANDED_BYTES - MAX_TEXT_BYTES - MAX_MANIFEST_BYTES) { "Backup attachments are too large" }
            require(attachment.sha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid attachment checksum" }
        }
    }

    private fun validateId(id: String) {
        require(id.isNotBlank() && id.length <= 256 && id.none { it.isISOControl() }) { "Invalid backup identifier" }
    }

    private fun validatePath(path: String) {
        require(path.isNotEmpty() && path.none { it == '\\' || it == ':' || it.isISOControl() } &&
            path.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Unsafe backup path: $path" }
    }

    internal fun copyLimited(
        input: InputStream,
        output: OutputStream,
        limit: Long,
        onChunk: (ByteArray, Int) -> Unit = { _, _ -> },
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "Backup entry exceeds the size limit" }
            onChunk(buffer, count)
            output.write(buffer, 0, count)
        }
        return total
    }

    private fun legacyIdentifier(value: String) = UUID.nameUUIDFromBytes("moe-memos-legacy:$value".toByteArray(Charsets.UTF_8)).toString()
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
}
