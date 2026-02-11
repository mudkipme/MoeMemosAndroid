package me.mudkip.moememos.ui.page.common

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.mudkip.moememos.MainActivity
import me.mudkip.moememos.data.model.ShareContent
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.page.account.AccountPage
import me.mudkip.moememos.ui.page.account.AddAccountPage
import me.mudkip.moememos.ui.page.login.LoginPage
import me.mudkip.moememos.ui.page.memoinput.MemoInputPage
import me.mudkip.moememos.ui.page.memos.MemoDetailPage
import me.mudkip.moememos.ui.page.memos.MemosPage
import me.mudkip.moememos.ui.page.memos.SearchPage
import me.mudkip.moememos.ui.page.memos.TagMemoPage
import me.mudkip.moememos.ui.page.resource.ResourceListPage
import me.mudkip.moememos.ui.page.settings.SettingsPage
import me.mudkip.moememos.ui.theme.MoeMemosTheme
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val userStateViewModel = LocalUserState.current
    val context = LocalContext.current
    var shareContent by remember { mutableStateOf<ShareContent?>(null) }

    CompositionLocalProvider(LocalRootNavController provides navController) {
        MoeMemosTheme {
            NavHost(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                navController = navController,
                startDestination = RouteName.MEMOS,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up,
                        initialOffset = { it / 4 }) + fadeIn()
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down,
                        targetOffset = { it / 4 }) + fadeOut()
                },
            ) {
                composable(RouteName.MEMOS) {
                    MemosPage()
                }

                composable(RouteName.SETTINGS) {
                    SettingsPage(navController = navController)
                }

                composable(RouteName.ADD_ACCOUNT) {
                    AddAccountPage(navController = navController)
                }

                composable(RouteName.LOGIN) {
                    LoginPage(navController = navController)
                }

                composable(RouteName.INPUT) {
                    MemoInputPage()
                }

                composable(RouteName.SHARE) {
                    MemoInputPage(shareContent = shareContent)
                }

                composable("${RouteName.EDIT}?memoId={id}"
                ) { entry ->
                    MemoInputPage(memoIdentifier = entry.arguments?.getString("id"))
                }

                composable(RouteName.RESOURCE) {
                    ResourceListPage(navController = navController)
                }

                composable("${RouteName.ACCOUNT}?accountKey={accountKey}") { entry ->
                    AccountPage(
                        navController = navController,
                        selectedAccountKey = entry.arguments?.getString("accountKey") ?: ""
                    )
                }

                composable(RouteName.SEARCH) {
                    SearchPage(navController = navController)
                }

                composable("${RouteName.TAG}/{tag}") { entry ->
                    val tag = entry.arguments?.getString("tag")?.let(Uri::decode) ?: ""
                    TagMemoPage(tag = tag, navController = navController)
                }

                composable("${RouteName.MEMO_DETAIL}?memoId={memoId}") { entry ->
                    val memoId = entry.arguments?.getString("memoId")
                    if (memoId != null) {
                        MemoDetailPage(navController = navController, memoIdentifier = Uri.decode(memoId))
                    }
                }
            }
        }
    }


    LaunchedEffect(Unit) {
        if (!userStateViewModel.hasAnyAccount()) {
            if (navController.currentDestination?.route != RouteName.ADD_ACCOUNT) {
                navController.navigate(RouteName.ADD_ACCOUNT) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            return@LaunchedEffect
        }
        userStateViewModel.loadCurrentUser()
    }

    fun handleIntent(intent: Intent) {
        when(intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                shareContent = ShareContent.parseIntent(intent)
                navController.navigate(RouteName.SHARE)
            }
            Intent.ACTION_VIEW -> {
                when (intent.getStringExtra("action")) {
                    "compose" -> navController.navigate(RouteName.INPUT)
                    "search" -> navController.navigate(RouteName.SEARCH)
                }
            }
            MainActivity.ACTION_NEW_MEMO -> {
                navController.navigate(RouteName.INPUT)
            }
            MainActivity.ACTION_EDIT_MEMO -> {
                val memoId = intent.getStringExtra(MainActivity.EXTRA_MEMO_ID)
                if (memoId != null) {
                    navController.navigate("${RouteName.EDIT}?memoId=$memoId")
                }
            }
            MainActivity.ACTION_VIEW_MEMO -> {
                val memoId = intent.getStringExtra(MainActivity.EXTRA_MEMO_ID)
                if (memoId != null) {
                    navController.navigate("${RouteName.MEMO_DETAIL}?memoId=${Uri.encode(memoId)}")
                }
            }
        }
    }

    LaunchedEffect(context) {
        if (context is ComponentActivity && context.intent != null) {
            handleIntent(context.intent)
        }
    }

    DisposableEffect(context) {
        val activity = context as? ComponentActivity

        val listener = Consumer<Intent> {
            handleIntent(it)
        }

        activity?.addOnNewIntentListener(listener)

        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }
}

val LocalRootNavController =
    compositionLocalOf<NavHostController> { error(me.mudkip.moememos.R.string.nav_host_controller_not_found.string) }
