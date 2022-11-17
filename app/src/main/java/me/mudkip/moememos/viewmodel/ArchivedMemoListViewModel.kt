package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemosRowStatus
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.ext.suspendOnErrorMessage
import javax.inject.Inject

@HiltViewModel
class ArchivedMemoListViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {
    var memos = mutableStateListOf<Memo>()
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    fun loadMemos() = viewModelScope.launch {
        memoRepository.loadMemos(rowStatus = MemosRowStatus.ARCHIVED).suspendOnSuccess {
            memos.clear()
            memos.addAll(data)
            errorMessage = null
        }.suspendOnErrorMessage {
            errorMessage = it
        }
    }

    suspend fun restoreMemo(memoId: Long) = withContext(viewModelScope.coroutineContext) {
        memoRepository.restoreMemo(memoId).suspendOnSuccess {
            memos.removeIf { it.id == memoId }
        }
    }

    suspend fun deleteMemo(memoId: Long) = withContext(viewModelScope.coroutineContext) {
        memoRepository.deleteMemo(memoId).suspendOnSuccess {
            memos.removeIf { it.id == memoId }
        }
    }
}

val LocalArchivedMemos = compositionLocalOf<ArchivedMemoListViewModel> { error("Archived memo list view model not found") }