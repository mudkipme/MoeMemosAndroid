package me.mudkip.moememos.data.service

import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.model.SyncStatus
import me.mudkip.moememos.data.repository.AbstractMemoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class MemoService @Inject constructor(
    private val accountService: AccountService,
) {
    private var lastSyncTime = 0L
    private val syncThreshold = 5000L
    private val syncMutex = Mutex()

    val repository: AbstractMemoRepository
        get() = runBlocking {
            accountService.getRepository()
        }

    val syncStatus: Flow<SyncStatus> = accountService.currentAccount.flatMapLatest {
        accountService.getRepository().syncStatus
    }

    val memos: Flow<List<MemoEntity>> = accountService.currentAccount.flatMapLatest {
        accountService.getRepository().observeMemos()
    }

    suspend fun sync(force: Boolean): ApiResponse<Unit> {
        return syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && (now - lastSyncTime) <= syncThreshold) {
                return@withLock ApiResponse.Success(Unit)
            }

            val repo = accountService.getRepository()
            val result = repo.sync()
            if (result is ApiResponse.Success) {
                lastSyncTime = now
            }
            result
        }
    }
}
