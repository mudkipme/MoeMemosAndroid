package me.mudkip.moememos.ui.page.memos

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.mudkip.moememos.ui.component.SideDrawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemosPage() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val memosNavController = rememberNavController()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                memosNavController = memosNavController,
                drawerState = drawerState
            )
        }
    ) {
        MemosNavigation(
            drawerState = drawerState,
            navController = memosNavController
        )
    }
}