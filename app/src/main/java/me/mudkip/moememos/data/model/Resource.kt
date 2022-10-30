package me.mudkip.moememos.data.model

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class Resource(
    val id: Long,
    val createdTs: Long,
    val creatorId: Long,
    val filename: String,
    val size: Long,
    val type: String,
    val updatedTs: Long
) {
    fun uri(host: String): Uri {
        return Uri.parse(host)
            .buildUpon().appendPath("o").appendPath("r")
            .appendPath(id.toString()).appendPath(filename).build()
    }
}
