package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.DailyUsageStat
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemosRowStatus
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.ext.suspendOnErrorMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class MemosViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {

    var memos = mutableStateListOf<Memo>()
        private set
    var tags = mutableStateListOf<String>()
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var matrix by mutableStateOf(DailyUsageStat.initialMatrix)
        private set

    init {
        snapshotFlow { memos.toList() }
            .onEach { matrix = calculateMatrix() }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        refreshing = true
        loadMemos().invokeOnCompletion {
            refreshing = false
        }
    }

    fun loadMemos() = viewModelScope.launch {
        memoRepository.loadMemos(rowStatus = MemosRowStatus.NORMAL).suspendOnSuccess {
            memos.clear()
            memos.addAll(data)
            errorMessage = null
        }.suspendOnErrorMessage {
            errorMessage = it
        }
    }

    fun loadTags() = viewModelScope.launch {
        memoRepository.getTags().suspendOnSuccess {
            tags.clear()
            tags.addAll(data)
        }
    }

    suspend fun updateMemoPinned(memoId: Long, pinned: Boolean) = withContext(viewModelScope.coroutineContext) {
        memoRepository.updatePinned(memoId, pinned).suspendOnSuccess {
            updateMemo(data)
        }
    }

    suspend fun editMemo(memoId: Long, content: String, resourceList: List<Resource>?): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        memoRepository.editMemo(memoId, content, resourceList?.map { it.id }).suspendOnSuccess {
            updateMemo(data)
        }
    }

    suspend fun archiveMemo(memoId: Long) = withContext(viewModelScope.coroutineContext) {
        memoRepository.archiveMemo(memoId).suspendOnSuccess {
            memos.removeIf { it.id == memoId }
        }
    }

    private fun updateMemo(memo: Memo) {
        val index = memos.indexOfFirst { it.id == memo.id }
        if (index != -1) {
            memos[index] = memo
        }
    }

    private fun calculateMatrix(): List<DailyUsageStat> {
        val countMap = HashMap<LocalDate, Int>()

        for (memo in memos) {
            val date = LocalDateTime.ofEpochSecond(memo.createdTs, 0, OffsetDateTime.now().offset).toLocalDate()
            countMap[date] = (countMap[date] ?: 0) + 1
        }

        return DailyUsageStat.initialMatrix.map {
            it.copy(count = countMap[it.date] ?: 0)
        }
    }
}

val LocalMemos = compositionLocalOf<MemosViewModel> { error("Memos view model not found") }