package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.model.Resource
import javax.inject.Inject

class ResourceRepository @Inject constructor(private val memosApiService: MemosApiService) {
    suspend fun loadResources(): ApiResponse<List<Resource>> = memosApiService.call { api ->
        api.getResources().mapSuccess { data }
    }
}