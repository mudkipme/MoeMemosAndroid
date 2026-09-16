package me.mudkip.moememos.ui.page.common

import androidx.activity.BackEventCompat
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/** Shared page motion for both the root stack and the drawer's nested stack. */
@Composable
internal fun MemosNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize().clipToBounds()
            .background(MaterialTheme.colorScheme.surface),
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, tween(300, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            slideOutOfContainer(
                SlideDirection.Start, tween(300, easing = FastOutSlowInEasing),
                targetOffset = { it / 4 },
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                SlideDirection.End, tween(300, easing = FastOutSlowInEasing),
                initialOffset = { it / 4 },
            )
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, tween(300, easing = FastOutSlowInEasing))
        },
        // Navigation 2.10 has separate predictive defaults: an opaque scale to
        // 70% would disappear at completion. Slide fully out instead, revealing
        // the previous page, and let NavHost handle seeking, cancellation and pop.
        predictivePopEnterTransition = { edge ->
            slideIntoContainer(
                if (edge == BackEventCompat.EDGE_RIGHT) SlideDirection.Left else SlideDirection.Right,
                tween(300, easing = LinearEasing),
                initialOffset = { it / 4 },
            )
        },
        predictivePopExitTransition = { edge ->
            slideOutOfContainer(
                if (edge == BackEventCompat.EDGE_RIGHT) SlideDirection.Left else SlideDirection.Right,
                tween(300, easing = LinearEasing),
            )
        },
        builder = builder,
    )
}
