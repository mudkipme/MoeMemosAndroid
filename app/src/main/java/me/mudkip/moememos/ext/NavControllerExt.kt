package me.mudkip.moememos.ext

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import me.mudkip.moememos.ui.page.common.RouteName

fun NavController.popBackStackIfLifecycleIsResumed(lifecycleOwner: LifecycleOwner? = null) {
    if (lifecycleOwner?.lifecycle?.currentState === Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

/**
 * Opens the editor for [memoId] unless it is already the current screen. A second editor for the
 * same memo (a quick double tap with the single-tap gesture, or a widget tap while the editor is
 * open) keeps its own copy of the text: after Send you land on the first one, still showing the old
 * text, and leaving or editing it writes that old text back over what you just sent.
 */
fun NavController.navigateToMemoEditor(memoId: String) {
    val current = currentBackStackEntry
    if (current?.destination?.route == "${RouteName.EDIT}?memoId={id}" &&
        current.arguments?.getString("id") == memoId
    ) {
        return
    }
    navigate("${RouteName.EDIT}?memoId=${Uri.encode(memoId)}")
}
