package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.api.SignInInput
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.data.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UserStateViewModel @Inject constructor(
    private val memosApiService: MemosApiService,
    private val userRepository: UserRepository
) : ViewModel() {

    var currentUser: User? by mutableStateOf(null)
    val host: String get() = memosApiService.host ?: ""

    suspend fun loadCurrentUser(): ApiResponse<User> = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        userRepository.getCurrentUser().suspendOnSuccess {
            currentUser = data
        }
    }

    suspend fun login(host: String, email: String, password: String): ApiResponse<User> = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        try {
            val resp = memosApiService.createClient(host).signIn(SignInInput(email, password)).mapSuccess { data }
            if (resp.isSuccess) {
                memosApiService.update(host)
                currentUser = resp.getOrNull()
            }
            resp
        } catch (e: Throwable) {
            ApiResponse.error(e)
        }
    }

    suspend fun logout() = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        memosApiService.call { it.logout() }
    }
}

val LocalUserState = compositionLocalOf<UserStateViewModel> { error("User state not found") }