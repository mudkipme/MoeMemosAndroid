package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.model.User
import javax.inject.Inject

class UserRepository @Inject constructor(private val memosApiService: MemosApiService) {
    suspend fun getCurrentUser(): ApiResponse<User> = memosApiService.call { api ->
        api.me()
    }
}