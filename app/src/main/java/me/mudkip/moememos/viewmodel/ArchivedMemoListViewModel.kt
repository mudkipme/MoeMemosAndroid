package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.suspendOnErrorMessage
import javax.inject.Inject

@HiltViewModel
class ArchivedMemoListViewModel @Inject constructor(
    private val memoService: MemoService
) : ViewModel() {
    var memos = mutableStateListOf<MemoEntity>()
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    fun loadMemos() = viewModelScope.launch {
        memoService.repository.listArchivedMemos().suspendOnSuccess {
            memos.clear()
            memos.addAll(data)
            errorMessage = null
        }.suspendOnErrorMessage {
            errorMessage = it
        }
    }

    suspend fun restoreMemo(identifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.repository.restoreMemo(identifier).suspendOnSuccess {
            memos.removeIf { it.identifier == identifier }
        }
    }

    suspend fun deleteMemo(identifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.repository.deleteMemo(identifier).suspendOnSuccess {
            memos.removeIf { it.identifier == identifier }
        }
    }
}

val LocalArchivedMemos =
    compositionLocalOf<ArchivedMemoListViewModel> { error(me.mudkip.moememos.R.string.archived_memo_list_view_model_not_found.string) }
