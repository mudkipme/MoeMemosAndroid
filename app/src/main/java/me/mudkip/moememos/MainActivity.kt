package me.mudkip.moememos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import me.mudkip.moememos.ui.page.common.Navigation
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemosViewModel
import me.mudkip.moememos.viewmodel.UserStateViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val userStateViewModel: UserStateViewModel by viewModels()
    private val memosViewModel: MemosViewModel by viewModels()

    companion object {
        const val ACTION_NEW_MEMO = "me.mudkip.moememos.action.NEW_MEMO"
        const val ACTION_EDIT_MEMO = "me.mudkip.moememos.action.EDIT_MEMO"
        const val EXTRA_MEMO_ID = "memoId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CompositionLocalProvider(
                LocalUserState provides userStateViewModel,
                LocalMemos provides memosViewModel
            ) {
                Navigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
