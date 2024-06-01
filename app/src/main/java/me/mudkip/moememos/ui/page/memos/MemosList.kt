package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import me.mudkip.moememos.ui.component.MemosCard
import me.mudkip.moememos.viewmodel.LocalMemos
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemosList(
    contentPadding: PaddingValues,
    swipeEnabled: Boolean = true,
    tag: String? = null,
    searchString: String? = null
) {
    val viewModel = LocalMemos.current
    val refreshState = rememberPullToRefreshState(enabled = { swipeEnabled })
    val filteredMemos = remember(viewModel.memos.toList(), tag, searchString) {
        val pinned = viewModel.memos.filter { it.pinned }
        val nonPinned = viewModel.memos.filter { !it.pinned }
        var fullList = pinned + nonPinned

        tag?.let { tag ->
            fullList = fullList.filter { memo ->
                memo.content.contains("#$tag") ||
                    memo.content.contains("#$tag/")
            }
        }

        searchString?.let { searchString ->
            if (searchString.isNotEmpty()) {
                fullList = fullList.filter { memo ->
                    memo.content.contains(searchString, true)
                }
            }
        }

        fullList
    }
    val lazyListState = rememberLazyListState()
    var listTopId: String? by rememberSaveable {
        mutableStateOf(null)
    }

    if (refreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadMemos().invokeOnCompletion {
                refreshState.endRefresh()
            }
        }
    }
    
    Box {
        LazyColumn(
            modifier = Modifier
                .consumeWindowInsets(contentPadding)
                .fillMaxSize()
                .nestedScroll(refreshState.nestedScrollConnection),
            contentPadding = contentPadding,
            state = lazyListState
        ) {
            items(filteredMemos, key = { it.identifier }) { memo ->
                MemosCard(memo, previewMode = true)
            }
        }
        
        PullToRefreshContainer(
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter).padding(contentPadding),
        )
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Timber.d(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }

    LaunchedEffect(filteredMemos.firstOrNull()?.identifier) {
        if (listTopId != null && filteredMemos.isNotEmpty() && listTopId != filteredMemos.first().identifier) {
            lazyListState.scrollToItem(0)
        }

        listTopId = filteredMemos.firstOrNull()?.identifier
    }
}