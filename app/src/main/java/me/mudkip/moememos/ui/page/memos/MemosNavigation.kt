package me.mudkip.moememos.ui.page.memos

import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.mudkip.moememos.ui.page.common.RouteName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemosNavigation(
    drawerState: DrawerState,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = RouteName.MEMOS
    ) {
        composable(
            RouteName.MEMOS,
        ) {
            MemosHomePage(
                drawerState = drawerState
            )
        }

        composable(
            RouteName.ARCHIVED
        ) {
            ArchivedMemoPage(
                drawerState = drawerState
            )
        }

        composable(
            "${RouteName.TAG}/{tag}"
        ) { entry ->
            TagMemoPage(
                drawerState = drawerState,
                tag = entry.arguments?.getString("tag") ?: ""
            )
        }
    }
}