package me.mudkip.moememos.ext

import android.graphics.Bitmap
import androidx.core.graphics.scale
import java.lang.Double.min

fun Bitmap.scaleTo(width: Int, height: Int): Bitmap {
    val scaleFactor = min(width.toDouble() / this.width, height.toDouble() / this.height)
    return this.scale((this.width * scaleFactor).toInt(), (this.height * scaleFactor).toInt())
}