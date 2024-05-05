package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import me.mudkip.moememos.R
import me.mudkip.moememos.data.api.SignInInput
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemosAccount
import me.mudkip.moememos.data.model.Status
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.data.repository.UserRepository
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.ext.string
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class UserStateViewModel @Inject constructor(
    private val accountService: AccountService,
    private val userRepository: UserRepository
) : ViewModel() {

    var currentUser: User? by mutableStateOf(null)
        private set
    var status: Status? by mutableStateOf(null)
        private set

    val host: String get() = accountService.currentAccount?.let {
        when (it) {
            is Account.Memos -> it.info.host
            else -> null
        }
    } ?: ""
    val okHttpClient: OkHttpClient get() = accountService.httpClient

    suspend fun loadCurrentUser(): ApiResponse<User> = withContext(viewModelScope.coroutineContext) {
        userRepository.getCurrentUser().suspendOnSuccess {
            currentUser = data
        }
    }

    suspend fun loadStatus(): ApiResponse<Status> = withContext(viewModelScope.coroutineContext) {
        userRepository.getStatus().suspendOnSuccess {
            status = data
        }
    }

    suspend fun login(host: String, username: String, password: String): ApiResponse<User> = withContext(viewModelScope.coroutineContext) {
        try {
            val hostUrl = host.toHttpUrlOrNull() ?: throw IllegalArgumentException()
            val (okHttpClient, client) = accountService.createMemosClient(host, null)
            val resp = client.signIn(SignInInput(username, username, password, true))
            okHttpClient.cookieJar.loadForRequest(hostUrl).forEach {
                if (it.name == "memos.access-token") {
                    accountService.addAccount(getAccount(host, it.value, resp.getOrThrow()))
                    currentUser = resp.getOrNull()
                    return@withContext resp
                }
            }
            throw MoeMemosException.invalidAccessToken
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    suspend fun loginWithAccessToken(host: String, accessToken: String): ApiResponse<User> = withContext(viewModelScope.coroutineContext) {
        try {
            val resp = accountService.createMemosClient(host, accessToken).second.me()
            accountService.addAccount(getAccount(host, accessToken, resp.getOrThrow()))
            currentUser = resp.getOrNull()
            resp
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    suspend fun logout() = withContext(viewModelScope.coroutineContext) {
        accountService.memosCall {
            it.logout()
        }.suspendOnSuccess {
            accountService.currentAccount?.let { account ->
                accountService.removeAccount(account)
            }
            currentUser = null
        }
    }

    private fun getAccount(host: String, accessToken: String, user: User): Account = Account.Memos(
        info = MemosAccount.newBuilder()
            .setHost(host)
            .setId(user.id)
            .setName(user.username)
            .setAvatarUrl(user.avatarUrl)
            .setAccessToken(accessToken)
            .build()
    )
}

val LocalUserState =
    compositionLocalOf<UserStateViewModel> { error(R.string.user_state_not_found.string) }