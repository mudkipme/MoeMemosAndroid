package me.mudkip.moememos.data.service

import kotlinx.coroutines.runBlocking
import me.mudkip.moememos.data.repository.AbstractMemoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoService @Inject constructor(
    private val accountService: AccountService
) {
    val repository: AbstractMemoRepository
        get() = runBlocking { accountService.getRepository() }
}