package me.mudkip.moememos.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

fun NavController.popBackStackIfLifecycleIsResumed(lifecycleOwner: LifecycleOwner? = null) {
    if (lifecycleOwner?.lifecycle?.currentState === Lifecycle.State.RESUMED) {
        popBackStack()
    }
}