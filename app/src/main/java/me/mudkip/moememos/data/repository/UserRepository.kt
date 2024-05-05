package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.Status
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.data.service.AccountService
import javax.inject.Inject

class UserRepository @Inject constructor(private val accountService: AccountService) {
    suspend fun getCurrentUser(): ApiResponse<User> = accountService.memosCall { api ->
        api.me()
    }

    suspend fun getStatus(): ApiResponse<Status> = accountService.memosCall { api ->
        api.status()
    }
}