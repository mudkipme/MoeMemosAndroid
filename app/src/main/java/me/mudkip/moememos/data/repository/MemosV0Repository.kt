package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.api.MemosRowStatus
import me.mudkip.moememos.data.api.MemosV0CreateMemoInput
import me.mudkip.moememos.data.api.MemosV0DeleteTagInput
import me.mudkip.moememos.data.api.MemosV0Memo
import me.mudkip.moememos.data.api.MemosV0PatchMemoInput
import me.mudkip.moememos.data.api.MemosV0Resource
import me.mudkip.moememos.data.api.MemosV0UpdateMemoOrganizerInput
import me.mudkip.moememos.data.api.MemosV0UpdateTagInput
import me.mudkip.moememos.data.api.MemosV0User
import me.mudkip.moememos.data.api.MemosVisibility
import me.mudkip.moememos.data.service.AccountService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class MemosV0Repository @Inject constructor(private val accountService: AccountService) {
    suspend fun loadMemos(rowStatus: MemosRowStatus? = null): ApiResponse<List<MemosV0Memo>> = accountService.memosCall { api ->
        api.listMemo(rowStatus = rowStatus)
    }

    suspend fun createMemo(content: String, resourceIdList: List<Long>? = null, visibility: MemosVisibility = MemosVisibility.PRIVATE): ApiResponse<MemosV0Memo> = accountService.memosCall { api ->
        api.createMemo(MemosV0CreateMemoInput(content, resourceIdList = resourceIdList, visibility = visibility))
    }

    suspend fun getTags(): ApiResponse<List<String>> = accountService.memosCall { api ->
        api.getTags()
    }

    suspend fun updateTag(name: String): ApiResponse<String> = accountService.memosCall { api ->
        api.updateTag(MemosV0UpdateTagInput(name))
    }

    suspend fun deleteTag(name: String): ApiResponse<Unit> = accountService.memosCall { api ->
        api.deleteTag(MemosV0DeleteTagInput(name))
    }

    suspend fun updatePinned(memoId: Long, pinned: Boolean): ApiResponse<MemosV0Memo> = accountService.memosCall { api ->
        api.updateMemoOrganizer(memoId, MemosV0UpdateMemoOrganizerInput(pinned = pinned))
    }

    suspend fun archiveMemo(memoId: Long): ApiResponse<MemosV0Memo> = accountService.memosCall { api ->
        api.patchMemo(memoId, MemosV0PatchMemoInput(id = memoId, rowStatus = MemosRowStatus.ARCHIVED))
    }

    suspend fun restoreMemo(memoId: Long): ApiResponse<MemosV0Memo> = accountService.memosCall { api ->
        api.patchMemo(memoId, MemosV0PatchMemoInput(id = memoId, rowStatus = MemosRowStatus.NORMAL))
    }

    suspend fun deleteMemo(memoId: Long): ApiResponse<Unit> = accountService.memosCall { api ->
        api.deleteMemo(memoId)
    }

    suspend fun editMemo(memoId: Long, content: String, resourceIdList: List<Long>? = null, visibility: MemosVisibility = MemosVisibility.PRIVATE): ApiResponse<MemosV0Memo> = accountService.memosCall { api ->
        api.patchMemo(memoId, MemosV0PatchMemoInput(id = memoId, content = content, resourceIdList = resourceIdList, visibility = visibility))
    }

    suspend fun listAllMemo(limit: Int? = null, offset: Int? = null, pinned: Boolean? = null, tag: String? = null, visibility: MemosVisibility? = null): ApiResponse<List<MemosV0Memo>> = accountService.memosCall { api ->
        api.listAllMemo(
            limit = limit,
            offset = offset,
            pinned = pinned,
            tag = tag,
            visibility = visibility
        )
    }

    suspend fun loadResources(): ApiResponse<List<MemosV0Resource>> = accountService.memosCall { api ->
        api.getResources()
    }

    suspend fun uploadResource(resourceData: ByteArray, filename: String, mediaType: MediaType): ApiResponse<MemosV0Resource> = accountService.memosCall { api ->
        val file = MultipartBody.Part.createFormData("file", filename, resourceData.toRequestBody(mediaType))
        api.uploadResource(file)
    }

    suspend fun deleteResource(resourceId: Long): ApiResponse<Unit> = accountService.memosCall { api ->
        api.deleteResource(resourceId)
    }

    suspend fun getCurrentUser(): ApiResponse<MemosV0User> = accountService.memosCall { api ->
        api.me()
    }
}