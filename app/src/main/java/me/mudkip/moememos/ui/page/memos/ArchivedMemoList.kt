package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.mudkip.moememos.ui.component.ArchivedMemoCard
import me.mudkip.moememos.viewmodel.ArchivedMemoListViewModel
import me.mudkip.moememos.viewmodel.LocalArchivedMemos

@Composable
fun ArchivedMemoList(
    viewModel: ArchivedMemoListViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    CompositionLocalProvider(LocalArchivedMemos provides viewModel) {
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(contentPadding),
            contentPadding = contentPadding
        ) {
            items(viewModel.memos, key = { it.identifier }) { memo ->
                ArchivedMemoCard(memo)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }
}