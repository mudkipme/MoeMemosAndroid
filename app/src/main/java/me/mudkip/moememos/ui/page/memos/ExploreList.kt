package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import me.mudkip.moememos.ui.component.ExploreMemoCard
import me.mudkip.moememos.viewmodel.ExploreViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreList(
    viewModel: ExploreViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val memos = viewModel.exploreMemos.collectAsLazyPagingItems()

    LazyColumn(
        modifier = Modifier.consumeWindowInsets(contentPadding),
        contentPadding = contentPadding
    ) {
        items(memos) { memo ->
            memo?.let {
                ExploreMemoCard(memo)
            }
        }
    }
}