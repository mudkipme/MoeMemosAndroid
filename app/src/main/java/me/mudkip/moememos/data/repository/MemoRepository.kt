package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.api.CreateMemoInput
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.model.Memo
import javax.inject.Inject

class MemoRepository @Inject constructor(private val memosApiService: MemosApiService) {
    suspend fun loadMemos(): ApiResponse<List<Memo>> = memosApiService.call { api ->
        api.listMemo().mapSuccess { data }
    }

    suspend fun createMemo(content: String): ApiResponse<Memo> = memosApiService.call { api ->
        api.createMemo(CreateMemoInput(content)).mapSuccess { data }
    }

    suspend fun getTags(): ApiResponse<List<String>> = memosApiService.call {
        api -> api.getTags().mapSuccess { data }
    }
}