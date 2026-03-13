package me.mudkip.moememos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.mudkip.moememos.data.datasource.EXPLORE_PAGE_SIZE
import me.mudkip.moememos.data.datasource.ExplorePagingSource
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.service.AccountService
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel @Inject constructor(
    accountService: AccountService
) : ViewModel() {
    val exploreMemos = accountService.currentAccount
        .flatMapLatest { account ->
            if (account == null || account is Account.Local) {
                return@flatMapLatest flowOf(PagingData.empty<Memo>())
            }

            val remoteRepository = accountService.getRemoteRepository()
                ?: return@flatMapLatest flowOf(PagingData.empty<Memo>())

            Pager(PagingConfig(pageSize = EXPLORE_PAGE_SIZE)) {
                ExplorePagingSource(remoteRepository)
            }.flow
        }
        .cachedIn(viewModelScope)
}
