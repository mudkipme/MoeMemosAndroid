package me.mudkip.moememos.data.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.AbstractMemoRepository

const val EXPLORE_PAGE_SIZE = 20

class ExplorePagingSource(
    private val memoRepository: AbstractMemoRepository
) : PagingSource<String, Memo>() {

    override fun getRefreshKey(state: PagingState<String, Memo>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Memo> {
        val response = memoRepository.listWorkspaceMemos(EXPLORE_PAGE_SIZE, params.key)

        return try {
            response.mapSuccess {
                LoadResult.Page(
                    data = this.first,
                    prevKey = null,
                    nextKey = this.second,
                )
            }.getOrThrow()
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}