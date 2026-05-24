package me.mudkip.moememos.ui.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.map
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Settings as AppSettings
import me.mudkip.moememos.ext.settingsDataStore
import timber.log.Timber

private const val APP_LOCK_TIMEOUT_MILLIS = 60_000L
private const val AUTH_PROMPT_THROTTLE_MILLIS = 1_000L
private fun appLockAuthenticators(): Int {
    val biometricOrDeviceCredential =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P ||
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
    ) {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    } else {
        biometricOrDeviceCredential
    }
}

object AppLockSession {
    var isUnlocked by mutableStateOf(false)
        private set
    var isAuthenticating by mutableStateOf(false)
        private set
    var lockGeneration by mutableIntStateOf(0)
        private set
    var foregroundGeneration by mutableIntStateOf(0)
        private set

    private var lastBackgroundAtMillis by mutableLongStateOf(0L)
    private var lastPromptAtMillis by mutableLongStateOf(0L)
    private var lastAutoPromptedLockGeneration by mutableIntStateOf(-1)
    private var handledForegroundGeneration by mutableIntStateOf(-1)

    fun markAppForegrounded() {
        foregroundGeneration++
    }

    fun markAppBackgrounded() {
        if (!isAuthenticating) {
            lastBackgroundAtMillis = System.currentTimeMillis()
        }
    }

    fun hasPendingTimeoutLock(appLockEnabled: Boolean): Boolean {
        val backgroundAt = lastBackgroundAtMillis
        if (!appLockEnabled || !isUnlocked || backgroundAt <= 0L) {
            return false
        }
        if (handledForegroundGeneration == foregroundGeneration) {
            return false
        }
        return System.currentTimeMillis() - backgroundAt >= APP_LOCK_TIMEOUT_MILLIS
    }

    fun consumeForegroundTimeout(appLockEnabled: Boolean) {
        if (handledForegroundGeneration == foregroundGeneration) {
            return
        }
        handledForegroundGeneration = foregroundGeneration

        val backgroundAt = lastBackgroundAtMillis
        lastBackgroundAtMillis = 0L
        if (!appLockEnabled || !isUnlocked || backgroundAt <= 0L) {
            return
        }
        if (System.currentTimeMillis() - backgroundAt >= APP_LOCK_TIMEOUT_MILLIS) {
            lock()
        }
    }

    fun lock() {
        lastBackgroundAtMillis = 0L
        isUnlocked = false
        isAuthenticating = false
        lockGeneration++
    }

    fun markAuthenticated() {
        lastBackgroundAtMillis = 0L
        isUnlocked = true
        isAuthenticating = false
    }

    fun markAuthenticationFinished() {
        isAuthenticating = false
    }

    fun shouldAutoPrompt(): Boolean {
        return lastAutoPromptedLockGeneration != lockGeneration
    }

    fun markAutoPrompted() {
        lastAutoPromptedLockGeneration = lockGeneration
    }

    fun tryBeginAuthentication(): Boolean {
        val now = System.currentTimeMillis()
        if (isAuthenticating || now - lastPromptAtMillis < AUTH_PROMPT_THROTTLE_MILLIS) {
            return false
        }
        lastPromptAtMillis = now
        isAuthenticating = true
        return true
    }
}

object AppLockAuthenticator {
    fun canAuthenticate(context: Context): Boolean {
        return runCatching {
            BiometricManager.from(context).canAuthenticate(appLockAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (CharSequence?) -> Unit,
    ) {
        if (!AppLockSession.tryBeginAuthentication()) {
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.app_lock_prompt_title))
            .setSubtitle(activity.getString(R.string.app_lock_prompt_subtitle))
            .setAllowedAuthenticators(appLockAuthenticators())
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockSession.markAuthenticated()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    AppLockSession.markAuthenticationFinished()
                    onError(errString)
                }

                override fun onAuthenticationFailed() {
                    // The system prompt remains visible for retry attempts.
                }
            }
        )

        runCatching {
            prompt.authenticate(promptInfo)
        }.onFailure { throwable ->
            AppLockSession.markAuthenticationFinished()
            Timber.d(throwable, "Failed to show app lock prompt")
            onError(activity.getString(R.string.app_lock_prompt_failed))
        }
    }
}

@Composable
fun AppLockGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsState = context.applicationContext.settingsDataStore.data
        .map<AppSettings, AppSettings?> { settings -> settings }
        .collectAsState(initial = null)
    val settings = settingsState.value
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (settings == null) {
        AppLockLoadingScreen()
        return
    }

    val appLockEnabled = settings.appLockEnabled
    val foregroundGeneration = AppLockSession.foregroundGeneration
    val pendingTimeoutLock = AppLockSession.hasPendingTimeoutLock(appLockEnabled)

    LaunchedEffect(appLockEnabled, foregroundGeneration) {
        AppLockSession.consumeForegroundTimeout(appLockEnabled)
    }

    val locked = appLockEnabled && (!AppLockSession.isUnlocked || pendingTimeoutLock)
    if (!locked) {
        content()
        return
    }

    val activity = remember(context) { context.findFragmentActivity() }
    val canAuthenticate = remember(context, foregroundGeneration) {
        AppLockAuthenticator.canAuthenticate(context)
    }
    val canStartAuthentication = activity != null && canAuthenticate
    var promptError by remember(AppLockSession.lockGeneration) { mutableStateOf<CharSequence?>(null) }

    fun requestAuthentication() {
        val fragmentActivity = activity ?: run {
            promptError = context.getString(R.string.app_lock_unavailable)
            return
        }
        if (!canStartAuthentication) {
            promptError = context.getString(R.string.app_lock_unavailable)
            return
        }
        AppLockAuthenticator.authenticate(
            activity = fragmentActivity,
            onSuccess = { promptError = null },
            onError = { error ->
                promptError = error ?: context.getString(R.string.app_lock_authentication_required)
            }
        )
    }

    LaunchedEffect(isResumed, canStartAuthentication, pendingTimeoutLock, AppLockSession.lockGeneration) {
        if (!pendingTimeoutLock && isResumed && canStartAuthentication && AppLockSession.shouldAutoPrompt()) {
            AppLockSession.markAutoPrompted()
            requestAuthentication()
        }
    }

    AppLockScreen(
        canStartAuthentication = canStartAuthentication,
        error = promptError,
        onUnlock = ::requestAuthentication,
        onOpenSecuritySettings = {
            runCatching {
                context.startActivity(Intent(AndroidSettings.ACTION_SECURITY_SETTINGS))
            }.onFailure { throwable ->
                Timber.d(throwable, "Failed to open security settings")
            }
        }
    )
}

@Composable
private fun AppLockLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppLockScreen(
    canStartAuthentication: Boolean,
    error: CharSequence?,
    onUnlock: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = contentColor.copy(alpha = 0.62f)
    val disabledContentColor = contentColor.copy(alpha = 0.36f)
    val errorColor = if (isDarkTheme) Color(0xFFFFB4AB) else Color(0xFFB3261E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_locked_title),
                style = MaterialTheme.typography.titleMedium,
                color = secondaryContentColor,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = if (canStartAuthentication) {
                    stringResource(R.string.app_locked_subtitle)
                } else {
                    stringResource(R.string.app_lock_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryContentColor,
                textAlign = TextAlign.Center,
            )
            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = error.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = errorColor,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedButton(
                onClick = onUnlock,
                enabled = canStartAuthentication && !AppLockSession.isAuthenticating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = contentColor,
                    disabledContentColor = disabledContentColor,
                )
            ) {
                Text(stringResource(R.string.unlock))
            }
            if (!canStartAuthentication) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenSecuritySettings,
                    colors = ButtonDefaults.textButtonColors(contentColor = secondaryContentColor)
                ) {
                    Text(stringResource(R.string.open_system_security_settings))
                }
            }
        }
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext: Context? = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return currentContext as? FragmentActivity
}
