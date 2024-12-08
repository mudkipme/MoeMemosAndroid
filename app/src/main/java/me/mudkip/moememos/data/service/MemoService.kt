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
    private var lastSyncTime = 0L
    private val syncThreshold = 5000L // 5 seconds

    val repository: AbstractMemoRepository
        get() = runBlocking { 
            val localRepo = accountService.getLocalRepository()
            
            // If online and enough time has passed since last sync
            val currentTime = System.currentTimeMillis()
            if (networkUtils.isOnline.value && (currentTime - lastSyncTime) > syncThreshold) {
                withContext(Dispatchers.IO) {
                    try {
                        accountService.syncMemos()
                        lastSyncTime = currentTime
                    } catch (e: Exception) {
                        e.printStackTrace() 
                    }
                }
            }
            
            localRepo
        }
}