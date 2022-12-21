package me.mudkip.moememos.data.model

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                            images.add(it)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                            images.add(it)
                        }
                    }
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                            images.addAll(it)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
                            for (item in it) {
                                if (item is Uri) {
                                    images.add(item)
                                }
                            }
                        }
                    }
                }
            }

            return ShareContent(text ?: "", images)
        }
    }
}