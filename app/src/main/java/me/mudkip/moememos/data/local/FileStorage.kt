package me.mudkip.moememos.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private fun accountDir(accountKey: String): File {
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(accountKey.toByteArray(Charsets.UTF_8))
        return File(context.filesDir, "resources/$encoded").also { it.mkdirs() }
    }

    fun saveFile(accountKey: String, content: ByteArray, filename: String): Uri {
        return saveFile(accountKey, filename) { output ->
            output.write(content)
        }
    }

    fun saveFile(accountKey: String, sourceUri: Uri, filename: String): Uri {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Unable to open URI for reading: $sourceUri")
        inputStream.use { input ->
            return saveFile(accountKey, input, filename)
        }
    }

    fun saveFile(accountKey: String, input: InputStream, filename: String): Uri {
        return saveFile(accountKey, filename) { output ->
            input.copyTo(output)
        }
    }

    private fun saveFile(accountKey: String, filename: String, writer: (java.io.OutputStream) -> Unit): Uri {
        val file = File(accountDir(accountKey), filename)
        file.outputStream().use(writer)
        return Uri.fromFile(file)
    }

    fun deleteFile(uri: Uri) {
        uri.path?.let { path ->
            File(path).delete()
        }
    }

    fun deleteAccountFiles(accountKey: String) {
        accountDir(accountKey).deleteRecursively()
    }
}
