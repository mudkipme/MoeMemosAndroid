package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.ext.suspendOnErrorMessage
import javax.inject.Inject


@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {
    var memos = mutableStateListOf<Memo>()
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    fun loadMemos() = viewModelScope.launch {
        memoRepository.listAllMemo().suspendOnSuccess {
            memos.clear()
            memos.addAll(data)
            errorMessage = null
        }.suspendOnErrorMessage {
            errorMessage = it
        }
    }
}