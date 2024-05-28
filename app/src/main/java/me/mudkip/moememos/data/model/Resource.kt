package me.mudkip.moememos.data.model

import android.net.Uri
import java.util.Date

data class Resource(
    val identifier: String,
    val date: Date,
    val filename: String,
    val mimeType: String,
    val uri: Uri,
)
