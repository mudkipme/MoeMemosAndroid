package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.User
import javax.inject.Inject

class UserRepository @Inject constructor(private val memosApiService: MemosApiService) {
    suspend fun getCurrentUser(): ApiResponse<User> {
        return memosApiService.memosApi?.let {
            it.me().mapSuccess { data }
        } ?: ApiResponse.error(MoeMemosException.notLogin)
    }
}