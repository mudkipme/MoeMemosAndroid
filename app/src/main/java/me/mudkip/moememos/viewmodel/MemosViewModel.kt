package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.ext.suspendOnErrorMessage
import javax.inject.Inject

@HiltViewModel
class MemosViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {

    var memos: List<Memo> by mutableStateOf(ArrayList())
        private set
    var tags: List<String> by mutableStateOf(ArrayList())
        private set
    var errorMessage: String? by mutableStateOf(null)

    suspend fun loadMemos() = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        memoRepository.loadMemos().suspendOnSuccess {
            memos = data
            errorMessage = null
        }.suspendOnErrorMessage {
            errorMessage = it
        }
    }

    suspend fun loadTags() = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        memoRepository.getTags().suspendOnSuccess {
            tags = data
        }
    }
}