package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import me.mudkip.moememos.viewmodel.MemosViewModel
import timber.log.Timber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemosList(
    contentPadding: PaddingValues,
    viewModel: MemosViewModel,
    swipeEnabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val refreshState = rememberSwipeRefreshState(viewModel.refreshing)

    SwipeRefresh(
        indicatorPadding = contentPadding,
        state = refreshState,
        swipeEnabled = swipeEnabled,
        onRefresh = {
            coroutineScope.launch {
                viewModel.refresh()
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.consumedWindowInsets(contentPadding),
            contentPadding = contentPadding
        ) {
            items(viewModel.memos) { memo ->
                MemosCard(memo)
            }
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Timber.d(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }
}