package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnError
import com.skydoves.sandwich.suspendOnException
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.MemoRepository
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {
    suspend fun createMemo(content: String): ApiResponse<Memo> {
        return viewModelScope.async(Dispatchers.IO) {
            return@async memoRepository.createMemo(content)
        }.await()
    }
}