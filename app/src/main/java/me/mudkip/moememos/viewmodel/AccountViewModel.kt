package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.getOrNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.api.MemosProfile
import me.mudkip.moememos.data.api.MemosV0Api
import me.mudkip.moememos.data.api.MemosV0User
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.api.MemosV1User
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.service.AccountService

@HiltViewModel(assistedFactory = AccountViewModel.AccountViewModelFactory::class)
class AccountViewModel @AssistedInject constructor(
    @Assisted val selectedAccountKey: String,
    private val accountService: AccountService
): ViewModel() {
    sealed class RemoteApi {
        class MemosV0(val api: MemosV0Api): RemoteApi()
        class MemosV1(val api: MemosV1Api): RemoteApi()
    }

    sealed class UserAndProfile {
        class MemosV0(val user: MemosV0User, val profile: MemosProfile): UserAndProfile()
        class MemosV1(val user: MemosV1User, val profile: MemosProfile): UserAndProfile()
    }

    @AssistedFactory
    interface AccountViewModelFactory {
        fun create(selectedAccountKey: String): AccountViewModel
    }

    private val selectedAccount = accountService.accounts.map { accounts ->
        accounts.firstOrNull { it.accountKey() == selectedAccountKey }
    }
    val selectedAccountState = selectedAccount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val memosApi = selectedAccount.map { account ->
        when (account) {
            is Account.MemosV0 -> {
                val (_, api) = accountService.createMemosV0Client(account.info.host, account.info.accessToken)
                return@map RemoteApi.MemosV0(api)
            }
            is Account.MemosV1 -> {
                val (_, api) = accountService.createMemosV1Client(account.info.host, account.info.accessToken)
                return@map RemoteApi.MemosV1(api)
            }
            else -> null
        }
    }

    var userAndProfile: UserAndProfile? by mutableStateOf(null)
        private set

    suspend fun loadUserAndProfile() = withContext(viewModelScope.coroutineContext) {
        when (val memosApi = memosApi.firstOrNull()) {
            is RemoteApi.MemosV0 -> {
                val user = memosApi.api.me().getOrNull()
                val profile = memosApi.api.status().getOrNull()?.profile
                if (user != null && profile != null) {
                    userAndProfile = UserAndProfile.MemosV0(user, profile)
                }
            }
            is RemoteApi.MemosV1 -> {
                val user = memosApi.api.getCurrentUser().getOrNull()?.user
                val profile = memosApi.api.getProfile().getOrNull()
                if (user != null && profile != null) {
                    userAndProfile = UserAndProfile.MemosV1(user, profile)
                }
            }
            else -> Unit
        }
    }
}
