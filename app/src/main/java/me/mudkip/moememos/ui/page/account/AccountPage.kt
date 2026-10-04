package me.mudkip.moememos.ui.page.account

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemosAccount
import me.mudkip.moememos.data.model.displayTitle
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.AccountViewModel
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.LocalMemos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPage(
    navController: NavHostController,
    selectedAccountKey: String
) {
    val viewModel = hiltViewModel<AccountViewModel, AccountViewModel.AccountViewModelFactory> { factory ->
        factory.create(selectedAccountKey)
    }
    val userStateViewModel = LocalUserState.current
    val memosViewModel = LocalMemos.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedAccount by viewModel.selectedAccountState.collectAsStateWithLifecycle()
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val memosAccount = selectedAccount.toMemosAccount()
    val isLocalAccount = selectedAccountKey == Account.Local().accountKey() || selectedAccount is Account.Local
    val showSwitchAccountButton = selectedAccountKey != currentAccount?.accountKey()
    val coroutineScope = rememberCoroutineScope()
    val accounts by userStateViewModel.accounts.collectAsStateWithLifecycle()
    val transferTargets = accounts.filter { it !is Account.Local }
    var transferInProgress by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/zip")) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        viewModel.exportLocalAccount(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) viewModel.prepareLocalImport(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = R.string.account_detail.string) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = R.string.back.string)
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLocalAccount) {
            LocalAccountPage(
                innerPadding = innerPadding,
                showSwitchAccountButton = showSwitchAccountButton,
                transferTargets = transferTargets,
                transferInProgress = transferInProgress,
                backupInProgress = viewModel.backupBusy,
                onSwitchAccount = {
                    coroutineScope.launch {
                        userStateViewModel.switchAccount(selectedAccountKey)
                            .onSuccess {
                                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                            }
                    }
                },
                onTransferLocalMemos = { target ->
                    transferInProgress = true
                    coroutineScope.launch {
                        val result = viewModel.transferLocalMemos(target.accountKey())
                        transferInProgress = false
                        result.onSuccess { count ->
                            if (count == 0) {
                                Toast.makeText(navController.context, R.string.transfer_local_memos_empty.string, Toast.LENGTH_SHORT).show()
                                return@onSuccess
                            }
                            val name = target.getAccountInfo()?.displayTitle().orEmpty()
                            Toast.makeText(
                                navController.context,
                                navController.context.getString(R.string.transfer_local_memos_success, count, name),
                                Toast.LENGTH_SHORT
                            ).show()
                            // Show the copies where they went; returning to the list syncs them up.
                            // The switch happens even if loading the user fails (server offline).
                            userStateViewModel.switchAccount(target.accountKey())
                            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                        }.onFailure { error ->
                            val message = error.localizedMessage ?: R.string.transfer_local_memos_failed.string
                            Toast.makeText(navController.context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onExportLocalAccount = {
                    val filename = "MoeMemos-Export-${exportTimestamp(Instant.now())}.zip"
                    exportLauncher.launch(filename)
                },
                onImportLocalAccount = {
                    importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                },
            )
        } else if (memosAccount != null) {
            MemosAccountPage(
                innerPadding = innerPadding,
                account = memosAccount,
                profile = viewModel.instanceProfile,
                okHttpClient = userStateViewModel.okHttpClient,
                showSwitchAccountButton = showSwitchAccountButton,
                onSwitchAccount = {
                    coroutineScope.launch {
                        userStateViewModel.switchAccount(selectedAccountKey)
                            .onSuccess {
                                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                            }
                    }
                },
                onSignOut = {
                    coroutineScope.launch {
                        userStateViewModel.logout(selectedAccountKey)
                        if (userStateViewModel.currentAccount.first() == null) {
                            navController.navigate(RouteName.ADD_ACCOUNT) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                        }
                    }
                }
            )
        } else {
            LazyColumn(contentPadding = innerPadding) {}
        }
    }

    LaunchedEffect(selectedAccountKey) {
        viewModel.loadInstanceProfile()
    }

    LaunchedEffect(viewModel.exportSucceeded) {
        if (viewModel.exportSucceeded) {
            Toast.makeText(navController.context, R.string.local_export_success.string, Toast.LENGTH_SHORT).show()
            viewModel.clearBackupMessage()
        }
    }

    LaunchedEffect(viewModel.importResult) {
        if (viewModel.importResult != null && currentAccount is Account.Local) {
            memosViewModel.refreshLocalSnapshot()
            memosViewModel.loadTags()
        }
    }

    viewModel.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportPreview,
            title = { Text(R.string.import_local_account.string) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(navController.context.getString(R.string.local_import_preview, preview.memos, preview.attachments))
                    Spacer(Modifier.height(12.dp))
                    Text(R.string.local_import_additive.string)
                    if (preview.existingMemos > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(navController.context.getString(R.string.local_import_existing, preview.existingMemos))
                    }
                    if (preview.legacy) {
                        Spacer(Modifier.height(12.dp))
                        Text(R.string.local_import_legacy.string)
                    }
                    if (preview.ignoredFiles > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(navController.context.getString(R.string.local_import_ignored, preview.ignoredFiles))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::restoreLocalImport) { Text(R.string.restore_local_backup.string) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissImportPreview) { Text(R.string.cancel.string) }
            },
        )
    }

    viewModel.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::clearBackupMessage,
            title = { Text(R.string.local_import_complete.string) },
            text = { Text(navController.context.getString(R.string.local_import_result,
                result.importedMemos, result.importedAttachments, result.skippedMemos, result.skippedAttachments)) },
            confirmButton = { TextButton(onClick = viewModel::clearBackupMessage) { Text(R.string.close.string) } },
        )
    }

    viewModel.backupError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearBackupMessage,
            title = { Text(R.string.local_backup_failed.string) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearBackupMessage) { Text(R.string.close.string) } },
        )
    }
}

private fun exportTimestamp(instant: Instant): String {
    return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

private fun Account?.toMemosAccount(): MemosAccount? = when (this) {
    is Account.MemosV0 -> info
    is Account.MemosV1 -> info
    else -> null
}
