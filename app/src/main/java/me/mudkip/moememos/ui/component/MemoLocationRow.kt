package me.mudkip.moememos.ui.component

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.MemoLocation

@Composable
fun MemoLocationRow(location: MemoLocation, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    Row(
        modifier = modifier.clickable(enabled = onClick != null || location.hasValidCoordinates) {
            if (onClick != null) {
                onClick()
            } else {
                val coordinates = "${location.latitude},${location.longitude}"
                val query = Uri.encode("$coordinates (${location.displayText})")
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "geo:$coordinates?q=$query".toUri()))
                } catch (_: ActivityNotFoundException) {
                    // A browser remains available when no application handles geo URIs.
                    try {
                        val mapUri = "https://www.openstreetmap.org/?mlat=${location.latitude}&mlon=${location.longitude}#map=16/${location.latitude}/${location.longitude}".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                    } catch (_: ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, R.string.location_no_map_app, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = stringResource(R.string.memo_location), tint = MaterialTheme.colorScheme.primary)
        Text(location.displayText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
