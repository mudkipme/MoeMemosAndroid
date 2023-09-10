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
    val updatedTs: Long,
    val externalLink: String?,
    val publicId: String?
) {
    fun uri(host: String): Uri {
        if (!externalLink.isNullOrEmpty()) {
            return Uri.parse(externalLink)
        }
        if (!publicId.isNullOrEmpty()) {
            return Uri.parse(host)
                .buildUpon().appendPath("o").appendPath("r")
                .appendPath(id.toString()).appendPath(publicId).build()
        }
        return Uri.parse(host)
            .buildUpon().appendPath("o").appendPath("r")
            .appendPath(id.toString()).appendPath(filename).build()
    }
}
