package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.api.CreateMemoInput
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Memo
import javax.inject.Inject

class MemoRepository @Inject constructor(private val memosApiService: MemosApiService) {
    suspend fun loadMemos(): ApiResponse<List<Memo>> {
        return memosApiService.memosApi?.let { it.listMemo().mapSuccess { data } }
            ?: ApiResponse.error(MoeMemosException.notLogin)
    }

    suspend fun createMemo(content: String): ApiResponse<Memo> {
        return memosApiService.memosApi?.let {
            it.createMemo(CreateMemoInput(content)).mapSuccess { data }
        } ?: ApiResponse.error(MoeMemosException.notLogin)
    }
}