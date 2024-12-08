package me.mudkip.moememos.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val filesDir: File
        get() = File(context.filesDir, "resources").also { it.mkdirs() }

    fun saveFile(content: ByteArray, filename: String): Uri {
        val file = File(filesDir, filename)
        file.writeBytes(content)
        return Uri.fromFile(file)
    }

    fun deleteFile(uri: Uri) {
        uri.path?.let { path ->
            File(path).delete()
        }
    }
}