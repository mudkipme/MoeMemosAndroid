package me.mudkip.moememos.viewmodel

import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.api.MemosProfile
import me.mudkip.moememos.data.api.MemosV0Api
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.data.service.LocalBackupService
import me.mudkip.moememos.data.service.LocalImportPreview
import me.mudkip.moememos.data.service.LocalImportResult
import me.mudkip.moememos.data.service.PreparedLocalImport

@HiltViewModel(assistedFactory = AccountViewModel.AccountViewModelFactory::class)
class AccountViewModel @AssistedInject constructor(
    @Assisted val selectedAccountKey: String,
    private val accountService: AccountService,
    private val localBackupService: LocalBackupService,
): ViewModel() {
    sealed class RemoteApi {
        class MemosV0(val api: MemosV0Api): RemoteApi()
        class MemosV1(val api: MemosV1Api): RemoteApi()
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

    var instanceProfile: MemosProfile? by mutableStateOf(null)
        private set

    suspend fun loadInstanceProfile() = withContext(viewModelScope.coroutineContext) {
        when (val memosApi = memosApi.firstOrNull()) {
            is RemoteApi.MemosV0 -> {
                val profile = memosApi.api.status().getOrNull()?.profile
                instanceProfile = profile
            }
            is RemoteApi.MemosV1 -> {
                val profile = memosApi.api.getProfile().getOrNull()
                instanceProfile = profile
            }
            else -> {
                instanceProfile = null
            }
        }
    }

    suspend fun transferLocalMemos(targetAccountKey: String): Result<Int> = withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
        if (selectedAccountKey != Account.Local().accountKey()) {
            return@withContext Result.failure(IllegalStateException("Transfer is available for local account only"))
        }
        runCatching {
            accountService.copyLocalMemosToAccount(targetAccountKey)
        }
    }

    var backupBusy by mutableStateOf(false)
        private set
    var importPreview: LocalImportPreview? by mutableStateOf(null)
        private set
    var importResult: LocalImportResult? by mutableStateOf(null)
        private set
    var backupError: String? by mutableStateOf(null)
        private set
    var exportSucceeded by mutableStateOf(false)
        private set
    private var preparedImport: PreparedLocalImport? = null

    fun exportLocalAccount(destinationUri: Uri) = backupOperation {
        localBackupService.export(destinationUri)
        exportSucceeded = true
    }

    fun prepareLocalImport(sourceUri: Uri) = backupOperation {
        dismissImportPreview()
        val prepared = localBackupService.prepareImport(sourceUri)
        preparedImport = prepared
        importPreview = prepared.preview
    }

    fun restoreLocalImport() = backupOperation {
        val prepared = preparedImport ?: return@backupOperation
        preparedImport = null
        importPreview = null
        try {
            importResult = localBackupService.restore(prepared)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { prepared.close() }
        }
    }

    fun dismissImportPreview() {
        val prepared = preparedImport
        preparedImport = null
        importPreview = null
        if (prepared != null) CoroutineScope(Dispatchers.IO).launch { prepared.close() }
    }

    fun clearBackupMessage() {
        backupError = null
        importResult = null
        exportSucceeded = false
    }

    private fun backupOperation(action: suspend () -> Unit) {
        if (backupBusy || selectedAccountKey != Account.Local().accountKey()) return
        backupBusy = true
        clearBackupMessage()
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                backupError = error.localizedMessage ?: "Unable to read or write local backup"
            } finally {
                backupBusy = false
            }
        }
    }

    override fun onCleared() {
        dismissImportPreview()
    }
}
