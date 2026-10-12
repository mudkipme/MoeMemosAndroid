package me.mudkip.moememos.ui.page.memoinput

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.MemoLocation
import me.mudkip.moememos.util.currentDeviceLocation
import me.mudkip.moememos.util.hasLocationPermission

@Composable
internal fun MemoLocationDialog(
    location: MemoLocation?,
    onConfirm: (MemoLocation?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var placeholder by rememberSaveable { mutableStateOf(location?.placeholder.orEmpty()) }
    var latitude by rememberSaveable { mutableStateOf(location?.latitude?.toString().orEmpty()) }
    var longitude by rememberSaveable { mutableStateOf(location?.longitude?.toString().orEmpty()) }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var request by remember { mutableStateOf<Job?>(null) }
    val candidate = MemoLocation.fromInput(placeholder, latitude, longitude)

    fun locate() {
        if (locating) return
        error = null
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) {
            error = R.string.location_services_disabled
            return
        }
        request = scope.launch {
            locating = true
            try {
                val fix = currentDeviceLocation(context)
                if (fix == null) {
                    error = R.string.location_unavailable
                } else {
                    latitude = fix.latitude.toString()
                    longitude = fix.longitude.toString()
                    if (placeholder.isBlank()) {
                        placeholder = MemoLocation(latitude = fix.latitude, longitude = fix.longitude).coordinatesText
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: SecurityException) {
                error = R.string.location_permission_denied
            } finally {
                locating = false
            }
        }
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it } || hasLocationPermission(context)) {
            locate()
        } else {
            error = R.string.location_permission_denied
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) request?.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            request?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.memo_location)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    enabled = !locating,
                    onClick = {
                        if (hasLocationPermission(context)) locate()
                        else permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                ) { Text(stringResource(R.string.use_current_location)) }
                if (locating) CircularProgressIndicator()
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                if (error == R.string.location_services_disabled) {
                    TextButton(onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (_: ActivityNotFoundException) {
                            error = R.string.location_unavailable
                        }
                    }) { Text(stringResource(R.string.location_settings)) }
                }
                OutlinedTextField(
                    value = placeholder, onValueChange = { placeholder = it },
                    label = { Text(stringResource(R.string.location_name)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !locating,
                )
                OutlinedTextField(
                    value = latitude, onValueChange = { latitude = it },
                    label = { Text(stringResource(R.string.latitude)) },
                    supportingText = { Text(stringResource(R.string.latitude_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !locating,
                )
                OutlinedTextField(
                    value = longitude, onValueChange = { longitude = it },
                    label = { Text(stringResource(R.string.longitude)) },
                    supportingText = { Text(stringResource(R.string.longitude_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !locating,
                )
                if (location != null) {
                    TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.remove_location)) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = candidate != null && !locating, onClick = { onConfirm(candidate) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
