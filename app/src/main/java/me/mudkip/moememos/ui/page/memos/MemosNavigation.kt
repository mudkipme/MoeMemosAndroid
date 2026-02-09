package me.mudkip.moememos.ui.page.memos

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
fun MemosNavigation(
    drawerState: DrawerState? = null,
    navController: NavHostController
) {
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsState()
    val hasExplore = currentAccount !is Account.Local

    NavHost(
        navController = navController,
        startDestination = RouteName.MEMOS
    ) {
        composable(
            RouteName.MEMOS,
        ) {
            MemosHomePage(
                drawerState = drawerState,
                navController = navController,
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

        composable(
            RouteName.EXPLORE
        ) {
            if (hasExplore) {
                ExplorePage(
                    drawerState = drawerState
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(RouteName.MEMOS) {
                        popUpTo(RouteName.EXPLORE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(RouteName.SEARCH) {
            SearchPage(navController = navController)
        }
    }
}
