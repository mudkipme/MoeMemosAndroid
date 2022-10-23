package me.mudkip.moememos.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.ext.DataStoreKeys
import me.mudkip.moememos.ext.dataStore
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(
    private val application: Application,
    private val memoRepository: MemoRepository
) : ViewModel() {
    val draft = application.applicationContext.dataStore.data.map { it[DataStoreKeys.Draft.key] }

    suspend fun createMemo(content: String): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        memoRepository.createMemo(content)
    }

    suspend fun editMemo(memoId: Long, content: String): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        memoRepository.editMemo(memoId, content)
    }

    fun updateDraft(content: String) = runBlocking {
        application.applicationContext.dataStore.edit {
            it[DataStoreKeys.Draft.key] = content
        }
    }
}