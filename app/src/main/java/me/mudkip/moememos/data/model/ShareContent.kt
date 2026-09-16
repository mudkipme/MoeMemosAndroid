package me.mudkip.moememos.data.model

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

data class ShareContent(
    val text: String = "",
    val images: List<Uri> = ArrayList()
) {
    companion object {
        fun parseIntent(intent: Intent): ShareContent {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val images = ArrayList<Uri>()

            when (intent.action) {
                Intent.ACTION_SEND -> {
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                        images.add(it)
                    }
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                        images.addAll(it.filterIsInstance<Uri>())
                    }
                }
            }

            return ShareContent(text ?: "", images)
        }
    }
}
