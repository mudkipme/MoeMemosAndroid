package me.mudkip.moememos.ext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.vector.ImageVector
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility

val MemoVisibility.icon: ImageVector get() = when (this) {
    MemoVisibility.PRIVATE -> Icons.Outlined.Lock
    MemoVisibility.PROTECTED -> Icons.Outlined.House
    MemoVisibility.PUBLIC -> Icons.Outlined.Public
}

val MemoVisibility.titleResource: Int get() = when (this) {
    MemoVisibility.PRIVATE -> R.string.memo_visibility_private
    MemoVisibility.PROTECTED -> R.string.memo_visibility_protected
    MemoVisibility.PUBLIC -> R.string.memo_visibility_public
}

fun Memo.getFullLink(host: String): String {
    return "${host}/m/${identifier}"
}
