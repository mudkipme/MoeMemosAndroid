package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.SyncStatusBadge
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.ManualSyncResult
import me.mudkip.moememos.viewmodel.LocalUserState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemosHomePage(
    drawerState: DrawerState? = null,
    navController: NavHostController
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val rootNavController = LocalRootNavController.current
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsState()
    val syncStatus by memosViewModel.syncStatus.collectAsState()

    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    var syncAlert by remember { mutableStateOf<HomeSyncAlert?>(null) }

    suspend fun requestManualSync(allowHigherV1Version: String? = null) {
        when (val result = memosViewModel.refreshMemos(allowHigherV1Version)) {
            ManualSyncResult.Completed -> Unit
            is ManualSyncResult.Blocked -> {
                syncAlert = HomeSyncAlert.Blocked(result.message)
            }
            is ManualSyncResult.RequiresConfirmation -> {
                syncAlert = HomeSyncAlert.RequiresConfirmation(result.version, result.message)
            }
            is ManualSyncResult.Failed -> {
                syncAlert = HomeSyncAlert.Failed(result.message)
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = R.string.memos.string) },
                navigationIcon = {
                    if (drawerState != null) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = R.string.menu.string)
                        }
                    }
                },
                actions = {
                    if (currentAccount !is Account.Local) {
                        SyncStatusBadge(
                            syncing = syncStatus.syncing,
                            unsyncedCount = syncStatus.unsyncedCount,
                            onSync = {
                                scope.launch {
                                    requestManualSync()
                                }
                            }
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate(RouteName.SEARCH)
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = R.string.search.string)
                    }
                }
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    rootNavController.navigate(RouteName.INPUT)
                },
                expanded = expandedFab,
                text = { Text(R.string.new_memo.string) },
                icon = { Icon(Icons.Filled.Add, contentDescription = R.string.compose.string) }
            )
        },

        content = { innerPadding ->
            MemosList(
                lazyListState = listState,
                contentPadding = innerPadding,
                onRefresh = { requestManualSync() }
            )
        }
    )

    when (val alert = syncAlert) {
        null -> Unit
        is HomeSyncAlert.Blocked -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                }
            )
        }
        is HomeSyncAlert.RequiresConfirmation -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.unsupported_memos_version_title.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            syncAlert = null
                            scope.launch {
                                requestManualSync(allowHigherV1Version = alert.version)
                            }
                        }
                    ) {
                        Text(R.string.still_sync.string)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.cancel.string)
                    }
                }
            )
        }
        is HomeSyncAlert.Failed -> {
            AlertDialog(
                onDismissRequest = { syncAlert = null },
                title = { Text(R.string.sync_failed.string) },
                text = { Text(alert.message) },
                confirmButton = {
                    TextButton(onClick = { syncAlert = null }) {
                        Text(R.string.close.string)
                    }
                }
            )
        }
    }
}

private sealed class HomeSyncAlert {
    data class Blocked(val message: String) : HomeSyncAlert()
    data class RequiresConfirmation(val version: String, val message: String) : HomeSyncAlert()
    data class Failed(val message: String) : HomeSyncAlert()
}
