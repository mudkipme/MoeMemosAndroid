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
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.skydoves.sandwich.onSuccess
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
    val userAndProfile = viewModel.userAndProfile
    val memosV0User = if (userAndProfile is AccountViewModel.UserAndProfile.MemosV0) userAndProfile.user else null
    val memosV1User = if (userAndProfile is AccountViewModel.UserAndProfile.MemosV1) userAndProfile.user else null
    val status = when (userAndProfile) {
        is AccountViewModel.UserAndProfile.MemosV0 -> userAndProfile.profile
        is AccountViewModel.UserAndProfile.MemosV1 -> userAndProfile.profile
        else -> null
    }
    val selectedAccount by viewModel.selectedAccountState.collectAsState()
    val currentAccount by userStateViewModel.currentAccount.collectAsState()
    val account = selectedAccount
    val isLocalAccount = selectedAccountKey == Account.Local().accountKey() || account is Account.Local
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
            if (isLocalAccount) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(CircleShape),
                                )
                                Text(
                                    R.string.local_account.string,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            Text(
                                R.string.local_account_description.string,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Text(
                                R.string.local_account_non_removable.string,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            } else if (userAndProfile != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            if (memosV0User != null) {
                                Text(memosV0User.displayName,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (memosV0User.displayName != memosV0User.displayEmail && memosV0User.displayEmail.isNotEmpty()) {
                                    Text(memosV0User.displayEmail,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            if (memosV1User != null) {
                                Text(memosV1User.displayName ?: "",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (memosV1User.displayName != memosV1User.email && !memosV1User.email.isNullOrEmpty()) {
                                    Text(memosV1User.email,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            if (account is Account.MemosV0) {
                                Text(
                                   account.info.host.toHttpUrlOrNull()?.host ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (account is Account.MemosV1) {
                                Text(
                                    account.info.host.toHttpUrlOrNull()?.host ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (status?.version?.isNotEmpty() == true) {
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
                                    Text("memos v${status.version}",
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
                                    .onSuccess {
                                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                                    }
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

            if (!isLocalAccount) {
                item {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                userStateViewModel.logout(selectedAccountKey)
                                if (userStateViewModel.currentAccount.first() == null) {
                                    navController.navigate(RouteName.ADD_ACCOUNT) {
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
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserAndProfile()
    }
}
