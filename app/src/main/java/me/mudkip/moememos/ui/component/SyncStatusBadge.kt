package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string

@Composable
fun SyncStatusBadge(
    syncing: Boolean,
    unsyncedCount: Int,
    onSync: () -> Unit
) {
    val indicatorSize: Dp = 20.dp

    if (syncing) {
        CircularProgressIndicator(
            modifier = Modifier.size(indicatorSize),
            strokeWidth = 2.dp
        )
        return
    }

    IconButton(onClick = onSync) {
        BadgedBox(
            badge = {
                if (unsyncedCount > 0) {
                    Badge {
                        Text(unsyncedCount.toString())
                    }
                }
            }
        ) {
            if (unsyncedCount > 0) {
                Icon(
                    Icons.Outlined.CloudOff,
                    contentDescription = R.string.sync_status_unsynced.string,
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    Icons.Outlined.Sync,
                    contentDescription = R.string.sync_status_sync_now.string
                )
            }
        }
    }
}
