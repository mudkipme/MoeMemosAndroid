package me.mudkip.moememos.ui.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.util.usesLocalNetwork

/** Requests LAN access only for a server that resolves to the local network. */
@Composable
fun rememberLocalNetworkPermissionRequest(): suspend (String) -> Boolean {
    val context = LocalContext.current
    val mutex = remember { Mutex() }
    var pendingResult by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    var showDeniedDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingResult?.complete(it)
        showDeniedDialog = !it
    }
    DisposableEffect(Unit) {
        onDispose { pendingResult?.cancel() }
    }

    if (showDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showDeniedDialog = false },
            title = { Text(R.string.local_network_access.string) },
            text = { Text(R.string.local_network_access_denied.string) },
            confirmButton = {
                TextButton(onClick = {
                    showDeniedDialog = false
                    context.startActivity(Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri(),
                    ))
                }) { Text(R.string.settings.string) }
            },
            dismissButton = {
                TextButton(onClick = { showDeniedDialog = false }) {
                    Text(R.string.cancel.string)
                }
            },
        )
    }

    return { serverUrl ->
        if (Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) == PackageManager.PERMISSION_GRANTED || !usesLocalNetwork(serverUrl)
        ) {
            true
        } else {
            mutex.withLock {
                val result = CompletableDeferred<Boolean>()
                pendingResult = result
                try {
                    launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    result.await()
                } finally {
                    pendingResult = null
                }
            }
        }
    }
}
