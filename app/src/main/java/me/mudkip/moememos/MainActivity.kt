package me.mudkip.moememos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import me.mudkip.moememos.ui.page.common.Navigation
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.UserStateViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val userStateViewModel: UserStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CompositionLocalProvider(LocalUserState provides userStateViewModel) {
                Navigation()
            }
        }
    }
}