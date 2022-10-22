package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.DailyUsageStat
import me.mudkip.moememos.data.model.Memo
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

    var memos: List<Memo> by mutableStateOf(ArrayList())
        private set
    var tags: List<String> by mutableStateOf(ArrayList())
        private set
    var errorMessage: String? by mutableStateOf(null)
    var refreshing by mutableStateOf(false)
    var matrix by mutableStateOf(DailyUsageStat.initialMatrix)

    init {
        snapshotFlow { memos }
            .onEach { matrix = calculateMatrix() }
            .launchIn(viewModelScope)
    }

    suspend fun refresh() {
        refreshing = true
        loadMemos()
        refreshing = false
    }

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