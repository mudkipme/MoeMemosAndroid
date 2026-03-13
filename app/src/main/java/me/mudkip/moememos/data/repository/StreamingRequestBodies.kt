package me.mudkip.moememos.data.repository

import android.util.Base64
import android.util.Base64OutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.InputStream
import java.io.OutputStream

class InputStreamRequestBody(
    private val mediaType: MediaType?,
    private val length: Long?,
    private val openInputStream: () -> InputStream
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long {
        return length ?: -1L
    }

    override fun writeTo(sink: BufferedSink) {
        openInputStream().use { input ->
            sink.writeAll(input.source())
        }
    }
}

class StreamingBase64JsonRequestBody(
    filename: String,
    type: String,
    memo: String?,
    private val contentLength: Long?,
    private val openInputStream: () -> InputStream
) : RequestBody() {
    private val prefix: String
    private val suffix: String

    init {
        val quotedFilename = Json.encodeToString(filename)
        val quotedType = Json.encodeToString(type)
        prefix = buildString {
            append('{')
            append("\"filename\":")
            append(quotedFilename)
            append(',')
            append("\"type\":")
            append(quotedType)
            append(',')
            append("\"content\":\"")
        }
        suffix = buildString {
            append('\"')
            if (memo != null) {
                append(',')
                append("\"memo\":")
                append(Json.encodeToString(memo))
            }
            append('}')
        }
    }

    override fun contentType(): MediaType {
        return "application/json; charset=utf-8".toMediaType()
    }

    override fun contentLength(): Long {
        val rawLength = contentLength
        if (rawLength == null || rawLength < 0L) {
            return -1L
        }
        val encodedLength = ((rawLength + 2L) / 3L) * 4L
        return prefix.encodeToByteArray().size.toLong() + encodedLength + suffix.encodeToByteArray().size.toLong()
    }

    override fun writeTo(sink: BufferedSink) {
        sink.writeUtf8(prefix)
        openInputStream().use { input ->
            val stream = Base64OutputStream(NoCloseOutputStream(sink.outputStream()), Base64.NO_WRAP)
            stream.use { base64 ->
                input.copyTo(base64)
            }
        }
        sink.writeUtf8(suffix)
    }
}

private class NoCloseOutputStream(
    private val delegate: OutputStream
) : OutputStream() {
    override fun write(b: Int) {
        delegate.write(b)
    }

    override fun write(b: ByteArray) {
        delegate.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        delegate.write(b, off, len)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        flush()
    }
}
