package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.MemoRepository
import javax.inject.Inject

@HiltViewModel
class MemosViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {
    var memos: List<Memo> by mutableStateOf(ArrayList())
    var errorMessage: String? by mutableStateOf(null)

    fun loadMemos() {
        viewModelScope.launch {
            memoRepository.loadMemos().suspendOnSuccess {
                memos = data
                errorMessage = null
            }.suspendOnError {
                errorMessage = response.errorBody().toString()
            }.suspendOnException {
                errorMessage = exception.localizedMessage
            }
        }
    }
}