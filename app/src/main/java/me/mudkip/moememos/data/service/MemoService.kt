package me.mudkip.moememos.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.repository.AbstractMemoRepository
import me.mudkip.moememos.util.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoService @Inject constructor(
    private val accountService: AccountService,
    private val networkUtils: NetworkUtils
) {
    val repository: AbstractMemoRepository
        get() = runBlocking { 
            val localRepo = accountService.getLocalRepository()
            
            // If online, trigger background sync but still return local repo
            if (networkUtils.isOnline.value) {
                withContext(Dispatchers.IO) {
                    accountService.syncMemos()
                }
            }
            
            localRepo
        }
}