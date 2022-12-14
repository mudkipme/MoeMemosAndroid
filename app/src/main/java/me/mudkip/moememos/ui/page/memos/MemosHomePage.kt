package me.mudkip.moememos.ui.page.memos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemosHomePage(
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    val rootNavController = LocalRootNavController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Memos") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        rootNavController.navigate(RouteName.SEARCH)
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    rootNavController.navigate(RouteName.INPUT)
                },
                text = { Text("New Memo") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Compose") }
            )
        },

        content = { innerPadding ->
            MemosList(
                contentPadding = innerPadding
            )
        }
    )
}