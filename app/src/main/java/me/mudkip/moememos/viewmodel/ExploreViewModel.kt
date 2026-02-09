package me.mudkip.moememos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import me.mudkip.moememos.data.datasource.EXPLORE_PAGE_SIZE
import me.mudkip.moememos.data.datasource.ExplorePagingSource
import me.mudkip.moememos.data.service.AccountService
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    accountService: AccountService
) : ViewModel() {
    private val remoteRepository = checkNotNull(runBlocking {
        accountService.getRemoteRepository()
    }) { "Explore is only available for remote accounts" }

    val exploreMemos = Pager(PagingConfig(pageSize = EXPLORE_PAGE_SIZE)) {
        ExplorePagingSource(remoteRepository)
    }.flow.cachedIn(viewModelScope)
}
