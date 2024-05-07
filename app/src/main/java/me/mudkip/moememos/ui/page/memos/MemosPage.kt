package me.mudkip.moememos.ui.page.memos

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.coroutines.launch
import me.mudkip.moememos.ui.component.SideDrawer

@Composable
fun MemosPage(
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val memosNavController = rememberNavController()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet {
                    SideDrawer(
                        memosNavController = memosNavController,
                    )
                }
            }
        ) {
            MemosNavigation(
                navController = memosNavController
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    SideDrawer(
                        memosNavController = memosNavController,
                        drawerState = drawerState
                    )
                }
            }
        ) {
            MemosNavigation(
                drawerState = drawerState,
                navController = memosNavController
            )
        }
    }
}