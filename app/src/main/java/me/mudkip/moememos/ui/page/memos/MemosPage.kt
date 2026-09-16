package me.mudkip.moememos.ui.page.memos

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import me.mudkip.moememos.ui.component.SideDrawer

@Composable
fun MemosPage() {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val memosNavController = rememberNavController()
    MemosNavigationDrawer(
        permanent = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),
        drawerContent = { drawerState ->
            SideDrawer(memosNavController = memosNavController, drawerState = drawerState)
        },
    ) { drawerState ->
        MemosNavigation(drawerState = drawerState, navController = memosNavController)
    }
}

@Composable
internal fun MemosNavigationDrawer(
    permanent: Boolean,
    drawerContent: @Composable (DrawerState?) -> Unit,
    content: @Composable (DrawerState?) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val currentContent = rememberUpdatedState(content)
    // Move the same composition between drawer layouts so navigation and pane
    // selection survive resizing across the permanent-drawer breakpoint.
    val navigation = remember {
        movableContentOf<DrawerState?> { state -> currentContent.value(state) }
    }
    if (permanent) {
        PermanentNavigationDrawer(
            drawerContent = { PermanentDrawerSheet { drawerContent(null) } },
        ) { navigation(null) }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerState = drawerState) { drawerContent(drawerState) }
            },
        ) { navigation(drawerState) }
    }
}
