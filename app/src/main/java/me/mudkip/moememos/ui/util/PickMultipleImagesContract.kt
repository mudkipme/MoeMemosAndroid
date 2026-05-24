package me.mudkip.moememos.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class PickMultipleImagesContract(
    private val requestedLimit: Int,
) : ActivityResultContract<Unit, List<Uri>>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, resolvedPickImagesLimit())
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val result = mutableListOf<Uri>()
        intent.clipData?.appendUrisTo(result)
        intent.data?.let { uri ->
            if (!result.contains(uri)) {
                result.add(uri)
            }
        }
        return result.take(requestedLimit.coerceAtLeast(0))
    }

    private fun resolvedPickImagesLimit(): Int {
        val safeRequestedLimit = requestedLimit.coerceAtLeast(1)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            minOf(safeRequestedLimit, MediaStore.getPickImagesMaxLimit())
        } else {
            safeRequestedLimit
        }
    }

    private fun ClipData.appendUrisTo(destination: MutableList<Uri>) {
        for (index in 0 until itemCount) {
            val uri = getItemAt(index).uri ?: continue
            if (!destination.contains(uri)) {
                destination.add(uri)
            }
        }
    }
}
