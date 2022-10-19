package me.mudkip.moememos.ui.page.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalUserState
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(
    navController: NavHostController
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val userStateViewModel = LocalUserState.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            if (userStateViewModel.currentUser == null) {
                item {
                    Button(
                        onClick = {
                            navController.navigate(RouteName.LOGIN)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Sign in",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            item {
                Text("About Moe Memos",
                    modifier = Modifier.fillMaxWidth().padding(24.dp, 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            item {
                Surface(onClick = { /*TODO*/ }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp, 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Web,
                            contentDescription = "Web",
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Website",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Surface(onClick = { /*TODO*/ }) {
                    Row(
                        modifier = Modifier.
                        fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(8.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Privacy",
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Privacy Policy",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (userStateViewModel.currentUser != null) {
                item {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                userStateViewModel.logout()
                                navController.navigate(RouteName.LOGIN) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Sign out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

        }
    }
}