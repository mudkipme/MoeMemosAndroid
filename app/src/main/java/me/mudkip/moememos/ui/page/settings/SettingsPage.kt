package me.mudkip.moememos.ui.page.settings

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import me.mudkip.moememos.ui.component.ActionIconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemoEditGesture
import me.mudkip.moememos.data.model.Settings
import me.mudkip.moememos.data.model.displayTitle
import me.mudkip.moememos.data.mtls.MtlsManager
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.settingsDataStore
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.MemosIcon
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.ui.security.AppLockAuthenticator
import me.mudkip.moememos.ui.security.AppLockSession
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.widget.WidgetUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    navController: NavHostController
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val userStateViewModel = LocalUserState.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val accounts by userStateViewModel.accounts.collectAsStateWithLifecycle()
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val settings by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = Settings())
    val appLockSupported = remember(context, AppLockSession.foregroundGeneration) {
        AppLockAuthenticator.canAuthenticate(context)
    }
    var showEditGestureDialog by remember { mutableStateOf(false) }
    var showRemoveCertificateDialog by remember { mutableStateOf(false) }
    var hasClientCertificate by remember {
        mutableStateOf(
            MtlsManager.hasSelectedCertificate(context)
        )
    }

    fun chooseClientCertificate() {
        val activity = context as? Activity
            ?: return

        MtlsManager.chooseCertificate(
            activity
        ) { success ->
            if (success) {
                hasClientCertificate = true
            }
        }
    }

    fun removeClientCertificate() {
        MtlsManager.clearSelectedCertificate(context)
        hasClientCertificate = false
        showRemoveCertificateDialog = false
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !appLockSupported) {
            return
        }
        if (enabled) {
            AppLockSession.lock()
        }
        scope.launch(Dispatchers.IO) {
            context.settingsDataStore.updateData { existingSettings ->
                existingSettings.copy(appLockEnabled = enabled)
            }
        }
    }

    val currentEditGesture = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.editGesture
        ?: MemoEditGesture.NONE
    val autosaveEnabled = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.autosave
        ?: false

    fun setAutosaveEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            context.settingsDataStore.updateData { existingSettings ->
                val userIndex = existingSettings.usersList.indexOfFirst { user ->
                    user.accountKey == existingSettings.currentUser
                }
                if (userIndex == -1) {
                    return@updateData existingSettings
                }
                val users = existingSettings.usersList.toMutableList()
                val user = users[userIndex]
                users[userIndex] = user.copy(
                    settings = user.settings.copy(autosave = enabled)
                )
                existingSettings.copy(usersList = users)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = R.string.settings.string) },
                navigationIcon = {
                    ActionIconButton(label = R.string.back.string, onClick = {
                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = R.string.back.string
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                Text(
                    R.string.accounts.string,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            accounts.forEach { account ->
                when (account) {
                    is Account.MemosV0 -> item {
                        SettingItem(
                            icon = MemosIcon,
                            text = account.info.displayTitle(),
                            subtitle = account.info.host,
                            trailingIcon = {
                                if (currentAccount?.accountKey() == account.accountKey()) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = R.string.account_selected.string,
                                        modifier = Modifier.padding(start = 16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }) {
                            navController.navigate("${RouteName.ACCOUNT}?accountKey=${account.accountKey()}")
                        }
                    }

                    is Account.MemosV1 -> item {
                        SettingItem(
                            icon = MemosIcon,
                            text = account.info.displayTitle(),
                            subtitle = account.info.host,
                            trailingIcon = {
                                if (currentAccount?.accountKey() == account.accountKey()) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = R.string.account_selected.string,
                                        modifier = Modifier.padding(start = 16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }) {
                            navController.navigate("${RouteName.ACCOUNT}?accountKey=${account.accountKey()}")
                        }
                    }

                    is Account.Local -> item {
                        SettingItem(
                            icon = Icons.Outlined.Home,
                            text = R.string.local_account.string,
                            trailingIcon = {
                                if (currentAccount?.accountKey() == account.accountKey()) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = R.string.account_selected.string,
                                        modifier = Modifier.padding(start = 16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }) {
                            navController.navigate("${RouteName.ACCOUNT}?accountKey=${account.accountKey()}")
                        }
                    }
                }
            }

            item {
                SettingItem(icon = Icons.Outlined.PersonAdd, text = R.string.add_account.string) {
                    navController.navigate(RouteName.ADD_ACCOUNT)
                }
            }

            item {
                Text(
                    R.string.preferences.string,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                SettingItem(
                    icon = Icons.Outlined.Edit,
                    text = R.string.edit_gesture.string,
                    trailingIcon = {
                        Text(
                            text = currentEditGesture.titleResource.string,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                ) {
                    showEditGestureDialog = true
                }
            }

            item {
                SettingSwitchItem(
                    icon = Icons.Outlined.Save,
                    text = R.string.autosave.string,
                    subtitle = R.string.autosave_summary.string,
                    checked = autosaveEnabled,
                    onCheckedChange = ::setAutosaveEnabled,
                )
            }

            item {
                Text(
                    R.string.security.string,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                val appLockToggleEnabled = appLockSupported || settings.appLockEnabled
                SettingSwitchItem(
                    icon = Icons.Outlined.Lock,
                    text = R.string.app_lock.string,
                    subtitle = if (appLockSupported) {
                        R.string.app_lock_summary.string
                    } else {
                        R.string.app_lock_unavailable_short.string
                    },
                    checked = settings.appLockEnabled,
                    enabled = appLockToggleEnabled,
                    onCheckedChange = ::setAppLockEnabled,
                )
            }
            item {
                SettingItem(
                    icon = Icons.Outlined.Lock,
                    text = R.string.client_certificate.string,
                    subtitle = if (hasClientCertificate) {
                        R.string.client_certificate_selected.string
                    } else {
                        R.string.client_certificate_not_selected.string
                    }
                ) {
                    chooseClientCertificate()
                }
            }

            if (hasClientCertificate) {
                item {
                    SettingItem(
                        icon = Icons.Outlined.Lock,
                        text = R.string.remove_client_certificate.string,
                        subtitle = R.string.remove_client_certificate_message.string
                    ) {
                        showRemoveCertificateDialog = true
                    }
                }
            }

            item {
                Text(
                    R.string.about.string,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                SettingItem(icon = Icons.Outlined.Web, text = R.string.website.string) {
                    uriHandler.openUri("https://memos.moe")
                }
            }

            item {
                SettingItem(icon = Icons.Outlined.Lock, text = R.string.privacy_policy.string) {
                    uriHandler.openUri("https://memos.moe/privacy")
                }
            }

            item {
                SettingItem(icon = Icons.Outlined.Source, text = R.string.acknowledgements.string) {
                    uriHandler.openUri("https://memos.moe/android-acknowledgements")
                }
            }

            item {
                SettingItem(
                    icon = Icons.Outlined.BugReport,
                    text = R.string.report_an_issue.string
                ) {
                    uriHandler.openUri("https://github.com/mudkipme/MoeMemosAndroid/issues")
                }
            }
        }
    }
    if (showRemoveCertificateDialog) {
        AlertDialog(
            onDismissRequest = {
                showRemoveCertificateDialog = false
            },
            title = {
                Text(R.string.remove_client_certificate_title.string)
            },
            text = {
                Text(
                    R.string.remove_client_certificate_message.string
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeClientCertificate()
                    }
                ) {
                    Text(R.string.remove.string)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveCertificateDialog = false
                    }
                ) {
                    Text(R.string.cancel.string)
                }
            }
        )
    }
    if (showEditGestureDialog) {
        AlertDialog(
            onDismissRequest = { showEditGestureDialog = false },
            title = { Text(R.string.edit_gesture.string) },
            text = {
                LazyColumn {
                    items(MemoEditGesture.entries.size) { index ->
                        val gesture = MemoEditGesture.entries[index]
                        TextButton(
                            onClick = {
                                showEditGestureDialog = false
                                scope.launch(Dispatchers.IO) {
                                    context.settingsDataStore.updateData { existingSettings ->
                                        val userIndex =
                                            existingSettings.usersList.indexOfFirst { user ->
                                                user.accountKey == existingSettings.currentUser
                                            }
                                        if (userIndex == -1) {
                                            return@updateData existingSettings
                                        }
                                        val users = existingSettings.usersList.toMutableList()
                                        val user = users[userIndex]
                                        users[userIndex] = user.copy(
                                            settings = user.settings.copy(editGesture = gesture)
                                        )
                                        existingSettings.copy(usersList = users)
                                    }
                                    WidgetUpdater.updateWidgets(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = gesture.titleResource.string,
                                color = if (gesture == currentEditGesture) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditGestureDialog = false }) {
                    Text(R.string.close.string)
                }
            }
        )
    }
}

private val MemoEditGesture.titleResource: Int
    get() = when (this) {
        MemoEditGesture.NONE -> R.string.edit_gesture_none
        MemoEditGesture.SINGLE -> R.string.edit_gesture_single
        MemoEditGesture.DOUBLE -> R.string.edit_gesture_double
        MemoEditGesture.LONG -> R.string.edit_gesture_long
    }
