package me.mudkip.moememos.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class LocalBackupArchiveTest {
    @get:Rule val temporary = TemporaryFolder()
    private val date = "2026-10-05T01:02:03.123456789Z"

    private fun sample(): LocalBackupData {
        val memo = LocalBackupMemo("stable-memo-id", "memos/0.md", date, "2026-10-05T02:00:00Z", "PRIVATE", true, true)
        val content = "# 旅行\r\n\r\n- [x] task\n```\n#literal\n```\n"
        val attachment = LocalBackupAttachment("stable-attachment-id", memo.id, "写真 09:30.png", "image/png", date, "attachments/0")
        val file = temporary.newFile().apply { writeBytes(byteArrayOf(0, 1, 2, -1, 42)) }
        return LocalBackupData(LocalBackupManifest(exportedAt = date, memos = listOf(memo), attachments = listOf(attachment)),
            mapOf(memo.contentFile to content), mapOf(attachment.file to file))
    }

    private fun write(data: LocalBackupData = sample()): File = temporary.newFile().also { archive ->
        archive.outputStream().use { LocalBackupArchive.write(data, it) }
    }

    private fun read(file: File) = LocalBackupArchive.read(file, temporary.newFolder(), ZoneId.of("Asia/Singapore"))

    private fun zip(entries: Map<String, ByteArray>): File = temporary.newFile().also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }

    private fun entries(file: File): MutableMap<String, ByteArray> = ZipFile(file).use { zip ->
        zip.entries().asSequence().associate { entry -> entry.name to zip.getInputStream(entry).use { it.readBytes() } }.toMutableMap()
    }

    @Test fun roundTripPreservesContentMetadataIdsAndAttachmentBytes() {
        val original = sample()
        val restored = read(write(original))
        assertFalse(restored.legacy)
        assertEquals(original.manifest.memos, restored.manifest.memos)
        assertEquals(original.contents, restored.contents)
        val attachment = restored.manifest.attachments.single()
        assertEquals(original.manifest.attachments.single().copy(size = 5, sha256 = attachment.sha256), attachment)
        assertEquals(64, attachment.sha256.length)
        assertArrayEquals(original.files.getValue(attachment.file).readBytes(), restored.files.getValue(attachment.file).readBytes())
    }

    @Test fun emptyLocalAccountAndUnattachedFilesRoundTrip() {
        val empty = LocalBackupData(LocalBackupManifest(exportedAt = date, memos = emptyList(), attachments = emptyList()), emptyMap(), emptyMap())
        assertTrue(read(write(empty)).manifest.memos.isEmpty())
        val data = sample()
        val detached = data.copy(manifest = data.manifest.copy(memos = emptyList(), attachments = data.manifest.attachments.map { it.copy(memoId = null) }), contents = emptyMap())
        assertNull(read(write(detached)).manifest.attachments.single().memoId)
    }

    @Test fun legacyRecoversDateCollisionsAndAssociatesMarkdownAndExtensionlessAttachments() {
        val file = zip(mapOf(
            "20261005-090203.md" to "first #旅行".toByteArray(),
            "20261005-090203_1.md" to "second".toByteArray(),
            "20261005-090203-1.png" to byteArrayOf(1),
            "20261005-090203-2" to byteArrayOf(2),
            "20261005-090203_1-1.md" to "a Markdown attachment".toByteArray(),
            "unrelated.txt" to byteArrayOf(3),
        ))
        val restored = read(file)
        assertTrue(restored.legacy)
        assertEquals(2, restored.manifest.memos.size)
        assertEquals(3, restored.manifest.attachments.size)
        assertEquals(1, restored.ignoredFiles)
        val first = restored.manifest.memos[0]
        val second = restored.manifest.memos[1]
        assertEquals("2026-10-05T01:02:03Z", first.createdAt)
        assertEquals(2, restored.manifest.attachments.count { it.memoId == first.id })
        assertEquals(1, restored.manifest.attachments.count { it.memoId == second.id })
        assertTrue(restored.manifest.memos.all { !it.pinned && !it.archived && it.visibility == "PRIVATE" })
        // Reopening/repacking an old export gives the same IDs, including attachments.
        val again = read(zip(entries(file).toSortedMap()))
        assertEquals(restored.manifest.memos.map { it.id }.toSet(), again.manifest.memos.map { it.id }.toSet())
        assertEquals(restored.manifest.attachments.map { it.id }.toSet(), again.manifest.attachments.map { it.id }.toSet())
    }

    @Test fun legacyInvalidDateFallsBackWithoutLosingTheMemo() {
        val restored = read(zip(mapOf("20260230-090203.md" to "recover me".toByteArray())))
        assertEquals("recover me", restored.contents.values.single())
        Instant.parse(restored.manifest.memos.single().createdAt)
    }

    @Test fun legacyCanBeExportedInTheNewFormat() {
        val restored = read(zip(mapOf("20261005-090203.md" to "memo".toByteArray(), "20261005-090203-1.pdf" to byteArrayOf(7))))
        val again = read(write(restored))
        assertFalse(again.legacy)
        assertEquals(restored.manifest, again.manifest)
        assertEquals(restored.contents, again.contents)
    }

    @Test fun rejectsUnknownFormatAndFutureVersionWithoutTryingLegacyRecovery() {
        val data = sample()
        for (manifest in listOf(data.manifest.copy(format = "memos-export"), data.manifest.copy(version = 2))) {
            val archive = zip(mapOf("manifest.json" to Json.encodeToString(manifest).toByteArray(), "20261005-090203.md" to "note".toByteArray()))
            assertThrows(IllegalArgumentException::class.java) { read(archive) }
        }
    }

    @Test fun rejectsUnrelatedZip() {
        assertThrows(IllegalArgumentException::class.java) { read(zip(mapOf("notes.md" to "not a local export".toByteArray()))) }
    }

    @Test fun rejectsUnsafePathsEvenForUnreferencedEntries() {
        for (name in listOf("../outside", "/absolute", "a/../../outside", "a\\outside", "C:outside")) {
            val entries = entries(write())
            entries[name] = byteArrayOf(1)
            assertThrows(IllegalArgumentException::class.java) { read(zip(entries)) }
        }
    }

    @Test fun rejectsMissingOrChangedAttachmentAndMissingMemoBeforeRestore() {
        val original = entries(write())
        val missingAttachment = original.toMutableMap().apply { remove("attachments/0") }
        val changedAttachment = original.toMutableMap().apply { put("attachments/0", byteArrayOf(8, 7, 6, 5, 4)) }
        val missingMemo = original.toMutableMap().apply { remove("memos/0.md") }
        for (entries in listOf(missingAttachment, changedAttachment, missingMemo)) {
            assertThrows(IllegalArgumentException::class.java) { read(zip(entries)) }
        }
    }

    @Test fun rejectsDuplicateIdsAndDanglingAttachmentAssociations() {
        val data = sample()
        for (manifest in listOf(
            data.manifest.copy(memos = data.manifest.memos + data.manifest.memos.single().copy(contentFile = "memos/1.md")),
            data.manifest.copy(attachments = data.manifest.attachments.map { it.copy(memoId = "unknown") }),
        )) {
            assertThrows(IllegalArgumentException::class.java) { write(data.copy(manifest = manifest)) }
        }
    }

    @Test fun rejectsAttachmentDisplayNamesThatCouldBecomePathsDuringTransfer() {
        val data = sample()
        for (name in listOf("../outside.txt", "a/../../outside", "a\\outside", ".", "..")) {
            val manifest = data.manifest.copy(attachments = data.manifest.attachments.map { it.copy(filename = name) })
            assertThrows(IllegalArgumentException::class.java) { write(data.copy(manifest = manifest)) }
        }
    }

    @Test fun boundedCopyRejectsOversizedInput() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalBackupArchive.copyLimited(ByteArrayInputStream(ByteArray(20)), ByteArrayOutputStream(), 10)
        }
    }

    @Test fun rejectsOversizedCompressedLegacyMemo() {
        val archive = zip(mapOf("20261005-090203.md" to ByteArray(4 * 1024 * 1024 + 1) { 'a'.code.toByte() }))
        assertThrows(IllegalArgumentException::class.java) { read(archive) }
    }

    @Test fun readAndWriteRespectCancellation() {
        val archive = write()
        assertThrows(InterruptedException::class.java) {
            LocalBackupArchive.read(archive, temporary.newFolder()) { throw InterruptedException() }
        }
        assertThrows(InterruptedException::class.java) {
            LocalBackupArchive.write(sample(), ByteArrayOutputStream()) { throw InterruptedException() }
        }
    }
}
