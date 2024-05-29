package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.api.MemosV0Status
import me.mudkip.moememos.data.api.MemosV0User
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.service.AccountService

@HiltViewModel(assistedFactory = AccountViewModel.AccountViewModelFactory::class)
class AccountViewModel @AssistedInject constructor(
    @Assisted val selectedAccountKey: String,
    private val accountService: AccountService
): ViewModel() {
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
                return@map api
            }
            else -> null
        }
    }

    var user: MemosV0User? by mutableStateOf(null)
        private set

    var status: MemosV0Status? by mutableStateOf(null)
        private set

    suspend fun loadUser(): ApiResponse<MemosV0User> = withContext(viewModelScope.coroutineContext) {
        memosApi.firstOrNull()?.me()?.suspendOnSuccess {
            user = data
        } ?: ApiResponse.exception(MoeMemosException.notLogin)
    }

    suspend fun loadStatus(): ApiResponse<MemosV0Status> = withContext(viewModelScope.coroutineContext) {
        memosApi.firstOrNull()?.status()?.suspendOnSuccess {
            status = data
        } ?: ApiResponse.exception(MoeMemosException.notLogin)
    }
}