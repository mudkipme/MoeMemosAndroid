package me.mudkip.moememos.data.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skydoves.sandwich.getOrElse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.MemoRepository
import me.mudkip.moememos.ext.getErrorMessage

const val EXPLORE_PAGE_SIZE = 20

class ExplorePagingSource constructor(
    private val memoRepository: MemoRepository
) : PagingSource<Int, Memo>() {

    override fun getRefreshKey(state: PagingState<Int, Memo>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Memo> {
        val pageNumber = params.key ?: 0
        val response = memoRepository.listAllMemo(EXPLORE_PAGE_SIZE, pageNumber * EXPLORE_PAGE_SIZE)

        return response.mapSuccess {
            val prevKey = if (pageNumber > 0) pageNumber - 1 else null
            val nextKey = if (this.isNotEmpty()) pageNumber + 1 else null

            LoadResult.Page(
                data = this,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        }.getOrElse {
            LoadResult.Error(MoeMemosException(response.getErrorMessage()))
        }
    }
}