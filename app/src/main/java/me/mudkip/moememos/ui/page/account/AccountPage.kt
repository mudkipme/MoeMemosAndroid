package me.mudkip.moememos.ui.page.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.MemosIcon
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.AccountViewModel
import me.mudkip.moememos.viewmodel.LocalUserState
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
    val user = viewModel.user
    val status = viewModel.status
    val selectedAccount by viewModel.selectedAccountState.collectAsState()
    val currentAccount by userStateViewModel.currentAccount.collectAsState()
    val account = selectedAccount
    val coroutineScope = rememberCoroutineScope()

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
        LazyColumn(contentPadding = innerPadding) {
            if (user != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Text(user.displayName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (user.displayName != user.displayEmail && user.displayEmail.isNotEmpty()) {
                                Text(user.displayEmail,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (account is Account.Memos) {
                                Text(
                                   account.info.host.toHttpUrlOrNull()?.host ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (status?.profile?.version?.isNotEmpty() == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Icon(
                                        imageVector = MemosIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .clip(CircleShape),
                                    )
                                    Text("memos v${status.profile.version}",
                                        modifier = Modifier.padding(top = 5.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedAccountKey != currentAccount?.accountKey()) {

                item {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                userStateViewModel.switchAccount(selectedAccountKey)
                                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Text(R.string.switch_account.string)
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            userStateViewModel.logout(selectedAccountKey)
                            if (userStateViewModel.currentAccount.first() == null) {
                                navController.navigate(RouteName.LOGIN) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Text(R.string.sign_out.string)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUser()
        viewModel.loadStatus()
    }
}