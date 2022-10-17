package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import me.mudkip.moememos.viewmodel.MemosViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemosList(
    contentPadding: PaddingValues,
    viewModel: MemosViewModel = hiltViewModel()
) {
    LazyColumn(
        modifier = Modifier.consumedWindowInsets(contentPadding),
        contentPadding = contentPadding
    ) {
        items(viewModel.memos) { memo ->
            MemosCard(memo)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMemos()
    }
}