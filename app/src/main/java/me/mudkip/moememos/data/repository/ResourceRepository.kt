package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.service.AccountService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ResourceRepository @Inject constructor(private val accountService: AccountService) {
    suspend fun loadResources(): ApiResponse<List<Resource>> = accountService.memosCall { api ->
        api.getResources()
    }

    suspend fun uploadResource(resourceData: ByteArray, filename: String, mediaType: MediaType): ApiResponse<Resource> = accountService.memosCall { api ->
        val file = MultipartBody.Part.createFormData("file", filename, resourceData.toRequestBody(mediaType))
        api.uploadResource(file)
    }

    suspend fun deleteResource(resourceId: Long): ApiResponse<Unit> = accountService.memosCall { api ->
        api.deleteResource(resourceId)
    }
}