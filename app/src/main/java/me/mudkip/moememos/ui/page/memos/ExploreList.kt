package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import me.mudkip.moememos.ui.util.edgeToEdgeContentPadding
import me.mudkip.moememos.ui.component.ExploreMemoCard
import me.mudkip.moememos.viewmodel.ExploreViewModel

@Composable
fun ExploreList(
    viewModel: ExploreViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val memos = viewModel.exploreMemos.collectAsLazyPagingItems()
    val listContentPadding = edgeToEdgeContentPadding(contentPadding)

    LazyColumn(
        modifier = Modifier.consumeWindowInsets(contentPadding),
        contentPadding = listContentPadding
    ) {
        items(memos.itemCount) { index ->
            val memo = memos[index]
            memo?.let {
                ExploreMemoCard(memo)
            }
        }
    }
}