package me.mudkip.moememos.ext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.vector.ImageVector
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.MemosVisibility

val MemosVisibility.icon: ImageVector get() = when (this) {
    MemosVisibility.PRIVATE -> Icons.Outlined.Lock
    MemosVisibility.PROTECTED -> Icons.Outlined.House
    MemosVisibility.PUBLIC -> Icons.Outlined.Public
}

val MemosVisibility.titleResource: Int get() = when (this) {
    MemosVisibility.PRIVATE -> R.string.memo_visibility_private
    MemosVisibility.PROTECTED -> R.string.memo_visibility_protected
    MemosVisibility.PUBLIC -> R.string.memo_visibility_public
}