package me.mudkip.moememos.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
        val file = File(accountDir(accountKey), filename)
        file.writeBytes(content)
        return Uri.fromFile(file)
    }

    fun deleteFile(uri: Uri) {
        uri.path?.let { path ->
            File(path).delete()
        }
    }
}
