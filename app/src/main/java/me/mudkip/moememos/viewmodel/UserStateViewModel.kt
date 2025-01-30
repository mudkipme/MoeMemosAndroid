package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import com.skydoves.sandwich.retrofit.raw
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.R
import me.mudkip.moememos.data.api.MemosV0SignInInput
import me.mudkip.moememos.data.api.MemosV0User
import me.mudkip.moememos.data.api.MemosV1User
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemosAccount
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.data.model.UserData
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.suspendOnNotLogin
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class UserStateViewModel @Inject constructor(
    private val accountService: AccountService
) : ViewModel() {

    var currentUser: User? by mutableStateOf(null)
        private set

    var host: String = ""
        private set
    val okHttpClient: OkHttpClient get() = accountService.httpClient
    val accounts = accountService.accounts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val currentAccount = accountService.currentAccount.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            accountService.currentAccount.collectLatest {
                host = when(it) {
                    is Account.MemosV0 -> it.info.host
                    else -> ""
                }
            }
        }
    }

    suspend fun loadCurrentUser(): ApiResponse<User> = withContext(viewModelScope.coroutineContext) {
        accountService.getRepository().getCurrentUser().suspendOnSuccess {
            currentUser = data
        }.suspendOnNotLogin {
            currentUser = null
        }
    }

    suspend fun loginMemos(host: String, username: String, password: String): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        try {
            when (accountService.detectAccountCase(host)) {
                UserData.AccountCase.MEMOS_V0 -> loginMemosV0(host, username, password)
                UserData.AccountCase.MEMOS_V1 -> loginMemosV1(host, username, password)
                else -> throw MoeMemosException.invalidServer
            }
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    private suspend fun loginMemosV0(host: String, username: String, password: String): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        try {
            val hostUrl = host.toHttpUrl()
            val (okHttpClient, client) = accountService.createMemosV0Client(host, null)
            val resp = client.signIn(MemosV0SignInInput(username, username, password, true))
            if (resp !is ApiResponse.Success) {
                return@withContext resp.mapSuccess {}
            }
            okHttpClient.cookieJar.loadForRequest(hostUrl).forEach {
                if (it.name == "memos.access-token") {
                    accountService.addAccount(getAccount(host, it.value, resp.data))
                    currentUser = resp.data.toUser()
                    return@withContext ApiResponse.Success(Unit)
                }
            }
            throw MoeMemosException.invalidAccessToken
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    private suspend fun loginMemosV1(host: String, username: String, password: String): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        try {
            val hostUrl = host.toHttpUrl()
            val (okHttpClient, client) = accountService.createMemosV1Client(host, null)
            val resp = client.signIn(username, password)
            if (resp !is ApiResponse.Success) {
                return@withContext resp.mapSuccess {}
            }
            okHttpClient.cookieJar.loadForRequest(hostUrl).forEach {
                if (it.name == "memos.access-token") {
                    accountService.addAccount(getAccount(host, it.value, resp.data))
                    return@withContext loadCurrentUser().mapSuccess {}
                }
            }
            // The HTTP response of Memos 0.22.0 uses incorrect cookie header
            if (resp.raw.headers("grpc-metadata-set-cookie").isNotEmpty()) {
                val accessToken = resp.raw.headers("grpc-metadata-set-cookie").first()
                    .substringBefore(';')
                    .substringAfter("memos.access-token=")
                if (accessToken.isNotEmpty()) {
                    accountService.addAccount(getAccount(host, accessToken, resp.data))
                    return@withContext loadCurrentUser().mapSuccess {}
                }
            }
            throw MoeMemosException.invalidAccessToken
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    suspend fun loginMemosWithAccessToken(host: String, accessToken: String): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        try {
            when (accountService.detectAccountCase(host)) {
                UserData.AccountCase.MEMOS_V1 -> loginMemosV1WithAccessToken(host, accessToken)
                UserData.AccountCase.MEMOS_V0 -> loginMemosV0WithAccessToken(host, accessToken)
                else -> throw MoeMemosException.invalidServer
            }
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    private suspend fun loginMemosV0WithAccessToken(host: String, accessToken: String): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        try {
            val resp = accountService.createMemosV0Client(host, accessToken).second.me()
            if (resp !is ApiResponse.Success) {
                return@withContext resp.mapSuccess {}
            }
            accountService.addAccount(getAccount(host, accessToken, resp.data))
            currentUser = resp.data.toUser()
            ApiResponse.Success(Unit)
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    private suspend fun loginMemosV1WithAccessToken(host: String, accessToken: String): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        try {
            val resp = accountService.createMemosV1Client(host, accessToken).second.authStatus()
            if (resp !is ApiResponse.Success) {
                return@withContext resp.mapSuccess {}
            }
            accountService.addAccount(getAccount(host, accessToken, resp.data))
            loadCurrentUser().mapSuccess {}
        } catch (e: Throwable) {
            ApiResponse.exception(e)
        }
    }

    suspend fun logout(accountKey: String) = withContext(viewModelScope.coroutineContext) {
        if (currentAccount.first()?.accountKey() == accountKey) {
            accountService.getRepository().logout()
            currentUser = null
        }
        accountService.removeAccount(accountKey)
    }

    suspend fun switchAccount(accountKey: String) = withContext(viewModelScope.coroutineContext) {
        accountService.switchAccount(accountKey)
        loadCurrentUser()
    }

    private fun getAccount(host: String, accessToken: String, user: MemosV0User): Account = Account.MemosV0(
        info = MemosAccount.newBuilder()
            .setHost(host)
            .setId(user.id)
            .setName(user.username)
            .setAvatarUrl(user.avatarUrl)
            .setAccessToken(accessToken)
            .build()
    )

    private fun getAccount(host: String, accessToken: String, user: MemosV1User): Account = Account.MemosV1(
        info = MemosAccount.newBuilder()
            .setHost(host)
            .setId(user.name.substringAfterLast('/').toLong())
            .setName(user.username)
            .setAvatarUrl(user.avatarUrl)
            .setAccessToken(accessToken)
            .build()
    )
}

val LocalUserState =
    compositionLocalOf<UserStateViewModel> { error(R.string.user_state_not_found.string) }