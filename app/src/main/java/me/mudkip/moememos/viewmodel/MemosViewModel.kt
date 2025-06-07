package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.widget.WidgetUpdater
import java.time.LocalDate
import java.time.OffsetDateTime
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MemosViewModel @Inject constructor(
    private val memoService: MemoService,
    private val accountService: AccountService,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    var memos = mutableStateListOf<Memo>()
        private set
    var tags = mutableStateListOf<String>()
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set
    var matrix by mutableStateOf(DailyUsageStat.initialMatrix)
        private set
    var host: String? by mutableStateOf(null)
        private set

    init {
        snapshotFlow { memos.toList() }
            .onEach { matrix = calculateMatrix() }
            .launchIn(viewModelScope)

        accountService.currentAccount
            .onEach { currentAccount ->
                val currentHost = currentAccount?.toUserData()?.memosV1?.host ?: let {
                    currentAccount?.toUserData()?.memosV0?.host
                }
                host = currentHost
            }
    }

    suspend fun loadMemos() = withContext(viewModelScope.coroutineContext) {
        memoService.repository.listMemos().suspendOnSuccess {
            memos.clear()
            memos.addAll(data)
            errorMessage = null
            loadHost()
            // Update widgets after loading memos
            WidgetUpdater.updateWidgets(appContext)
        }.suspendOnErrorMessage {
            errorMessage = it
        }
    }

    fun loadTags() = viewModelScope.launch {
        memoService.repository.listTags().suspendOnSuccess {
            tags.clear()
            tags.addAll(data)
        }
    }

    suspend fun loadHost() = withContext(viewModelScope.coroutineContext) {
        accountService.currentAccount.collect { currentAccount ->
            val currentHost = currentAccount?.toUserData()?.memosV1?.host ?: let {
                currentAccount?.toUserData()?.memosV0?.host
            }
            host = currentHost
        }
    }

    suspend fun deleteTag(name: String) = withContext(viewModelScope.coroutineContext) {
        memoService.repository.deleteTag(name).suspendOnSuccess {
            tags.remove(name)
        }
    }

    suspend fun updateMemoPinned(memoIdentifier: String, pinned: Boolean) = withContext(viewModelScope.coroutineContext) {
        memoService.repository.updateMemo(memoIdentifier, pinned = pinned).suspendOnSuccess {
            updateMemo(data)
            // Update widgets after pinning/unpinning a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun editMemo(memoIdentifier: String, content: String, resourceList: List<Resource>?, visibility: MemoVisibility): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        memoService.repository.updateMemo(memoIdentifier, content, resourceList, visibility).suspendOnSuccess {
            updateMemo(data)
            // Update widgets after editing a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun archiveMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.repository.archiveMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            // Update widgets after archiving a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    suspend fun deleteMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.repository.deleteMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            // Update widgets after deleting a memo
            WidgetUpdater.updateWidgets(appContext)
        }
    }

    private fun updateMemo(memo: Memo) {
        val index = memos.indexOfFirst { it.identifier == memo.identifier }
        if (index != -1) {
            memos[index] = memo
        }
    }

    private fun calculateMatrix(): List<DailyUsageStat> {
        val countMap = HashMap<LocalDate, Int>()

        for (memo in memos) {
            val date = memo.date.atZone(OffsetDateTime.now().offset).toLocalDate()
            countMap[date] = (countMap[date] ?: 0) + 1
        }

        return DailyUsageStat.initialMatrix.map {
            it.copy(count = countMap[it.date] ?: 0)
        }
    }
}

val LocalMemos =
    compositionLocalOf<MemosViewModel> { error(me.mudkip.moememos.R.string.memos_view_model_not_found.string) }
