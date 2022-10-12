package me.mudkip.moememos.ui.page.common

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import me.mudkip.moememos.ui.page.login.LoginPage
import me.mudkip.moememos.ui.page.memoinput.MemoInputPage
import me.mudkip.moememos.ui.page.memos.MemosPage
import me.mudkip.moememos.ui.page.settings.SettingsPage
import me.mudkip.moememos.ui.theme.MoeMemosTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Navigation() {
    val navController = rememberAnimatedNavController()

    MoeMemosTheme {
        AnimatedNavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            navController = navController,
            startDestination = RouteName.MEMOS,
            enterTransition = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(220, delayMillis = 90)
                        )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(90))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(220, delayMillis = 90)
                        )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(90))
            }
        ) {
            composable(
                RouteName.MEMOS,
            ) {
                MemosPage(navController = navController)
            }

            composable(
                RouteName.SETTINGS,
            ) {
                SettingsPage(navController = navController)
            }
            
            composable(
                RouteName.LOGIN
            ) {
                LoginPage(navController = navController)
            }
            
            composable(
                RouteName.INPUT
            ) {
                MemoInputPage(navController = navController)
            }
        }
    }
}