package me.mudkip.moememos.ui.page.account

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemosAccount
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.AccountViewModel
import me.mudkip.moememos.viewmodel.LocalUserState
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedAccount by viewModel.selectedAccountState.collectAsState()
    val currentAccount by userStateViewModel.currentAccount.collectAsState()
    val memosAccount = selectedAccount.toMemosAccount()
    val isLocalAccount = selectedAccountKey == Account.Local().accountKey() || selectedAccount is Account.Local
    val showSwitchAccountButton = selectedAccountKey != currentAccount?.accountKey()
    val coroutineScope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/zip")) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            val result = viewModel.exportLocalAccount(uri)
            result.onSuccess {
                Toast.makeText(navController.context, R.string.local_export_success.string, Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                val message = error.localizedMessage ?: R.string.local_export_failed.string
                Toast.makeText(navController.context, message, Toast.LENGTH_SHORT).show()
            }
        }
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
                onSwitchAccount = {
                    coroutineScope.launch {
                        userStateViewModel.switchAccount(selectedAccountKey)
                            .onSuccess {
                                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                            }
                    }
                },
                onExportLocalAccount = {
                    val filename = "MoeMemos-Export-${exportTimestamp(Instant.now())}.zip"
                    exportLauncher.launch(filename)
                }
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
