package me.mudkip.moememos.data.model

import android.net.Uri
import okhttp3.MediaType
import java.time.Instant
import java.util.Date

data class Resource(
    val identifier: String,
    val date: Instant,
    val filename: String,
    val mimeType: MediaType? = null,
    val uri: Uri,
)
